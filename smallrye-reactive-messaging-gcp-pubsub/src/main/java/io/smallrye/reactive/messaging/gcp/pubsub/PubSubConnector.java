package io.smallrye.reactive.messaging.gcp.pubsub;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
@Connector(PubSubConnector.CONNECTOR_NAME)
public class PubSubConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubConnector.class);

    static final String CONNECTOR_NAME = "smallrye-gcp-pubsub";

    @Inject
    @ConfigProperty(name = "gcp-pubsub-project-id")
    private String projectId;

    @Inject
    @ConfigProperty(name = "mock-pubsub-topics")
    private boolean mockPubSubTopics;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    @ConfigProperty(name = "mock-pubsub-host")
    private Optional<String> host;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    @ConfigProperty(name = "mock-pubsub-port")
    private Optional<Integer> port;

    @Inject
    private PubSubManager pubSubManager;

    private ExecutorService executorService;

    @PostConstruct
    public void initialize() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object context) {
        try {
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(final Config config) {
        final PubSubConfig pubSubConfig = new PubSubConfig(projectId, getTopic(config), getCredentialPath(config),
                getSubscription(config), mockPubSubTopics, host.orElse(null), port.orElse(null));

        return ReactiveStreams.fromCompletionStage(CompletableFuture.supplyAsync(() -> {
            createTopic(pubSubConfig);
            createSubscription(pubSubConfig);
            return pubSubConfig;
        }, executorService))
                .flatMapRsPublisher(
                        cfg -> Multi.createFrom().emitter(new PubSubSource(cfg, pubSubManager)));
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(final Config config) {
        final PubSubConfig pubSubConfig = new PubSubConfig(projectId, getTopic(config), getCredentialPath(config),
                mockPubSubTopics, host.orElse(null), port.orElse(null));

        return ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(message -> CompletableFuture.supplyAsync(() -> {
                    createTopic(pubSubConfig);
                    return await(pubSubManager.publisher(pubSubConfig).publish(buildMessage(message)));
                }, executorService))
                .ignore();
    }

    private void createTopic(final PubSubConfig config) {
        final TopicAdminClient topicAdminClient = pubSubManager.topicAdminClient(config);

        final ProjectTopicName topicName = ProjectTopicName.of(config.getProjectId(), config.getTopic());

        try {
            topicAdminClient.getTopic(topicName);
        } catch (final NotFoundException nf) {
            try {
                topicAdminClient.createTopic(topicName);
            } catch (final AlreadyExistsException ae) {
                LOGGER.trace("Topic {} already exists", topicName, ae);
            }
        }
    }

    private void createSubscription(final PubSubConfig config) {
        final SubscriptionAdminClient subscriptionAdminClient = pubSubManager.subscriptionAdminClient(config);

        final ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(config.getProjectId(),
                config.getSubscription());

        try {
            subscriptionAdminClient.getSubscription(subscriptionName);
        } catch (final NotFoundException e) {
            final PushConfig pushConfig = PushConfig.newBuilder()
                    .build();

            final ProjectTopicName topicName = ProjectTopicName.of(config.getProjectId(), config.getTopic());

            subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 0);
        }
    }

    private static String getTopic(final Config config) {
        final String topic = config.getOptionalValue("topic", String.class)
                .orElse(null);
        if (topic != null) {
            return topic;
        }

        return config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);
    }

    private static String getSubscription(final Config config) {
        return config.getValue("subscription", String.class);
    }

    private static Path getCredentialPath(final Config config) {
        return config.getOptionalValue("credential-path", String.class)
                .map(File::new)
                .map(File::toPath)
                .orElse(null);
    }

    private static PubsubMessage buildMessage(final Message<?> message) {
        if (message instanceof PubSubMessage) {
            return ((PubSubMessage) message).getMessage();
        } else {
            return PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(message.getPayload().toString()))
                    .build();
        }
    }

    private static <T> T await(final Future<T> future) {
        try {
            return future.get();
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
