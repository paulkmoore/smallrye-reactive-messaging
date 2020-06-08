package io.smallrye.reactive.messaging.helpers;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class BroadcastHelper {

    private BroadcastHelper() {
        // Avoid direct instantiation.
    }

    /**
     * <p>
     * Wraps an existing {@code Publisher} for broadcasting.
     * </p>
     *
     * @param publisher The publisher to be wrapped
     * @param numberOfSubscriberBeforeConnecting Number of subscribers that must be present before broadcast occurs.
     *        A value of 0 means any number of subscribers will trigger the broadcast.
     * @return The wrapped {@code Publisher} in a new {@code PublisherBuilder}
     */
    public static PublisherBuilder<? extends Message<?>> broadcastPublisher(Publisher<? extends Message<?>> publisher,
            int numberOfSubscriberBeforeConnecting) {
        Multi<? extends Message<?>> broadcastPublisher = Multi.createFrom().publisher(publisher);
        if (numberOfSubscriberBeforeConnecting != 0) {
            return ReactiveStreams.fromPublisher(broadcastPublisher
                    .broadcast().toAtLeast(numberOfSubscriberBeforeConnecting));
        } else {
            return ReactiveStreams.fromPublisher(broadcastPublisher.broadcast().toAllSubscribers());
        }
    }
}
