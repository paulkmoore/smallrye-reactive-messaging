package outbound;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class KafkaPriceProducer {

    private final Random random = new Random();

    @Outgoing("prices-out")
    public Multi<Double> generate() {
        // Build an infinite stream of random prices
        // It emits a price every 4 seconds
        return Multi.createFrom().ticks().every(Duration.ofSeconds(4))
                .map(x -> random.nextDouble());
    }

}
