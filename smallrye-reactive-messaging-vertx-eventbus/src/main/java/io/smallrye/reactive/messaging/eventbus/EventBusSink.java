package io.smallrye.reactive.messaging.eventbus;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.Vertx;

public class EventBusSink {

    private final String address;
    private final Vertx vertx;
    private final boolean publish;
    private final boolean expectReply;
    private final String codec;
    private final long timeout;

    EventBusSink(Vertx vertx, VertxEventBusConnectorOutgoingConfiguration config) {
        this.vertx = Objects.requireNonNull(vertx, "Vert.x instance must not be `null`");
        this.address = config.getAddress();
        this.publish = config.getPublish();
        this.expectReply = config.getExpectReply();

        if (this.publish && this.expectReply) {
            throw new IllegalArgumentException("Cannot enable `publish` and `expect-reply` at the same time");
        }

        this.codec = config.getCodec().orElse(null);
        this.timeout = config.getTimeout();
    }

    SubscriberBuilder<? extends Message<?>, Void> sink() {
        DeliveryOptions options = new DeliveryOptions();
        if (this.codec != null) {
            options.setCodecName(this.codec);
        }
        if (this.timeout != -1) {
            options.setSendTimeout(this.timeout);
        }

        return ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(msg -> {
                    // TODO support getting an EventBusMessage as message.
                    if (!this.publish) {
                        if (expectReply) {
                            return vertx.eventBus().request(address, msg.getPayload(), options).subscribeAsCompletionStage()
                                    .thenApply(m -> msg);
                        } else {
                            vertx.eventBus().sendAndForget(address, msg.getPayload(), options);
                            return CompletableFuture.completedFuture(msg);
                        }
                    } else {
                        vertx.eventBus().publish(address, msg.getPayload(), options);
                        return CompletableFuture.completedFuture(msg);
                    }
                })
                .ignore();
    }

}
