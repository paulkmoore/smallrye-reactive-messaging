package io.smallrye.reactive.messaging.camel.documentation;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PriceProducer {

    private final Random random = new Random();

    @Outgoing("prices")
    public Multi<String> generate() {
        // Build an infinite stream of random prices
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .onOverflow().drop()
                .onSubscribe().invoke(s -> s.request(Long.MAX_VALUE))
                .map(x -> random.nextDouble())
                .map(p -> Double.toString(p));
    }

}
