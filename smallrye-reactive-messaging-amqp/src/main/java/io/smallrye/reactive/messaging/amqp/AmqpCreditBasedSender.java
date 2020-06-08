package io.smallrye.reactive.messaging.amqp;

import static java.time.Duration.ofSeconds;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.vertx.amqp.impl.AmqpMessageBuilderImpl;
import io.vertx.mutiny.amqp.AmqpSender;

public class AmqpCreditBasedSender implements Processor<Message<?>, Message<?>>, Subscription {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpCreditBasedSender.class);

    private final ConnectionHolder holder;
    private final Uni<AmqpSender> retrieveSender;
    private final AtomicLong requested = new AtomicLong();
    private final AmqpConnectorOutgoingConfiguration configuration;
    private final AmqpConnector connector;

    private volatile AmqpSender sender;
    private final AtomicReference<Subscription> upstream = new AtomicReference<>();
    private final AtomicReference<Subscriber<? super Message<?>>> downstream = new AtomicReference<>();
    private final AtomicBoolean once = new AtomicBoolean();
    private final boolean durable;
    private final long ttl;
    private final boolean useAnonymousSender;
    private final String configuredAddress;

    public AmqpCreditBasedSender(AmqpConnector connector, ConnectionHolder holder,
            AmqpConnectorOutgoingConfiguration configuration, Uni<AmqpSender> retrieveSender) {
        this.connector = connector;
        this.holder = holder;
        this.retrieveSender = retrieveSender;
        this.configuration = configuration;
        this.durable = configuration.getDurable();
        this.ttl = configuration.getTtl();
        this.useAnonymousSender = configuration.getUseAnonymousSender();
        this.configuredAddress = configuration.getAddress().orElseGet(configuration::getChannel);
    }

    @Override
    public void subscribe(
            Subscriber<? super Message<?>> subscriber) {
        if (!downstream.compareAndSet(null, subscriber)) {
            Subscriptions.fail(subscriber, new IllegalStateException("Only one subscriber allowed"));
        } else {
            if (upstream.get() != null) {
                subscriber.onSubscribe(this);
            }
        }
    }

    private void getSenderAndCredits(Subscriber<? super Message<?>> subscriber) {
        retrieveSender.subscribe()
                .with(s -> {
                    sender = s;
                    holder.getContext().runOnContext(x -> setCreditsAndRequest());
                }, subscriber::onError);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.upstream.compareAndSet(null, subscription)) {
            Subscriber<? super Message<?>> subscriber = downstream.get();
            if (subscriber != null) {
                subscriber.onSubscribe(this);
            }
        } else {
            Subscriber<? super Message<?>> subscriber = downstream.get();
            if (subscriber != null) {
                subscriber.onSubscribe(Subscriptions.CANCELLED);
            }
        }
    }

    private long setCreditsAndRequest() {
        long credits = sender.remainingCredits();
        Subscription subscription = upstream.get();
        if (credits != 0L && subscription != Subscriptions.CANCELLED) {
            requested.set(credits);
            LOGGER.debug("Retrieved credits for channel `{}`: {}", configuration.getChannel(), credits);
            subscription.request(credits);
            return credits;
        }
        return 0L;
    }

    @Override
    public void onNext(Message<?> message) {
        if (isCancelled()) {
            return;
        }

        Subscriber<? super Message<?>> subscriber = this.downstream.get();
        send(sender, message, durable, ttl, configuredAddress, useAnonymousSender, configuration)
                .subscribe().with(
                        res -> {
                            subscriber.onNext(res);
                            if (requested.decrementAndGet() == 0) { // no more credit, request more
                                onNoMoreCredit();
                            }
                        },
                        subscriber::onError);
    }

    private void onNoMoreCredit() {
        LOGGER.debug("No more credit for channel {}, requesting more credits",
                configuration.getChannel());
        holder.getContext().runOnContext(x -> {
            if (isCancelled()) {
                return;
            }
            long c = setCreditsAndRequest();
            if (c == 0L) { // still no credits, schedule a periodic retry
                holder.getVertx().setPeriodic(configuration.getCreditRetrievalPeriod(), id -> {
                    if (setCreditsAndRequest() != 0L || isCancelled()) {
                        // Got our new credits or the application has been terminated,
                        // we cancel the periodic task.
                        holder.getVertx().cancelTimer(id);
                    }
                });
            }
        });
    }

    private boolean isCancelled() {
        Subscription subscription = upstream.get();
        return subscription == Subscriptions.CANCELLED || subscription == null;
    }

    @Override
    public void onError(Throwable throwable) {
        Subscription sub = upstream.getAndSet(Subscriptions.CANCELLED);
        Subscriber<? super Message<?>> subscriber = this.downstream.get();
        if (sub != null && sub != Subscriptions.CANCELLED && subscriber != null) {
            subscriber.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        Subscription sub = upstream.getAndSet(Subscriptions.CANCELLED);
        Subscriber<? super Message<?>> subscriber = this.downstream.get();
        if (sub != null && sub != Subscriptions.CANCELLED && subscriber != null) {
            subscriber.onComplete();
        }
    }

    @Override
    public void request(long l) {
        // Delay the retrieval of the sender and the request until we get a request.
        if (!once.getAndSet(true)) {
            getSenderAndCredits(downstream.get());
        }
    }

    @Override
    public void cancel() {
        Subscription sub = upstream.getAndSet(Subscriptions.CANCELLED);
        if (sub != null && sub != Subscriptions.CANCELLED) {
            sub.cancel();
        }
    }

    private Uni<Message<?>> send(AmqpSender sender, Message<?> msg, boolean durable, long ttl, String configuredAddress,
            boolean isAnonymousSender, AmqpConnectorCommonConfiguration configuration) {
        int retryAttempts = configuration.getReconnectAttempts();
        int retryInterval = configuration.getReconnectInterval();
        io.vertx.mutiny.amqp.AmqpMessage amqp;
        if (msg instanceof AmqpMessage) {
            amqp = ((AmqpMessage<?>) msg).getAmqpMessage();
        } else if (msg.getPayload() instanceof io.vertx.mutiny.amqp.AmqpMessage) {
            amqp = (io.vertx.mutiny.amqp.AmqpMessage) msg.getPayload();
        } else if (msg.getPayload() instanceof io.vertx.amqp.AmqpMessage) {
            amqp = new io.vertx.mutiny.amqp.AmqpMessage((io.vertx.amqp.AmqpMessage) msg.getPayload());
        } else {
            amqp = AmqpMessageConverter.convertToAmqpMessage(msg, durable, ttl);
        }

        String actualAddress = getActualAddress(msg, amqp, configuredAddress, isAnonymousSender);
        if (connector.getClients().isEmpty()) {
            LOGGER.error("The AMQP message to address `{}` has not been sent, the client is closed",
                    actualAddress);
            return Uni.createFrom().item(msg);
        }

        if (!actualAddress.equals(amqp.address())) {
            amqp = new io.vertx.mutiny.amqp.AmqpMessage(
                    new AmqpMessageBuilderImpl(amqp.getDelegate()).address(actualAddress).build());
        }

        LOGGER.debug("Sending AMQP message to address `{}` ",
                actualAddress);
        return sender.sendWithAck(amqp)
                .onFailure().retry().withBackOff(ofSeconds(1), ofSeconds(retryInterval)).atMost(retryAttempts)
                .onItemOrFailure().produceUni((success, failure) -> {
                    if (failure != null) {
                        return Uni.createFrom().completionStage(msg.nack(failure));
                    } else {
                        return Uni.createFrom().completionStage(msg.ack());
                    }
                })
                .onItem().apply(x -> msg);
    }

    private String getActualAddress(Message<?> message, io.vertx.mutiny.amqp.AmqpMessage amqp, String configuredAddress,
            boolean isAnonymousSender) {
        String address = amqp.address();
        if (address != null) {
            if (isAnonymousSender) {
                return address;
            } else {
                LOGGER.warn(
                        "Unable to use the address configured in the message ({}) - the connector is not using an anonymous sender, using {} instead",
                        address, configuredAddress);
                return configuredAddress;
            }

        }

        return message.getMetadata(OutgoingAmqpMetadata.class)
                .flatMap(o -> {
                    String addressFromMessage = o.getAddress();
                    if (addressFromMessage != null && !isAnonymousSender) {
                        LOGGER.warn(
                                "Unable to use the address configured in the message ({}) - the connector is not using an anonymous sender, using {} instead",
                                addressFromMessage, configuredAddress);
                        return Optional.empty();
                    }
                    return Optional.ofNullable(addressFromMessage);
                })
                .orElse(configuredAddress);
    }
}
