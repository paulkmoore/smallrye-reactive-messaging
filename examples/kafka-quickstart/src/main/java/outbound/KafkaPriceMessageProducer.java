package outbound;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class KafkaPriceMessageProducer {

    private final Random random = new Random();

    @Outgoing("prices-out")
    public Multi<Message<Double>> generate() {
        // Build an infinite stream of random prices
        // It emits a price every 8 seconds
        return Multi.createFrom().ticks().every(Duration.ofSeconds(8))
                .map(x -> Message.of(random.nextDouble()));
    }

}
