package io.smallrye.reactive.messaging.camel.documentation;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PriceMessageProducer {

    private final Random random = new Random();

    @Outgoing("prices")
    public Multi<Message<String>> generate() {
        // Build an infinite stream of random prices
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .on().overflow().drop()
                .map(x -> random.nextDouble())
                .map(p -> Double.toString(p))
                .map(Message::of);
    }

}
