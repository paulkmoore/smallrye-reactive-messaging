package inbound;

import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Optional;

public class AmqpMetadataExample {

    public void metadata() {
        Message<Double> incoming = Message.of(12.0);
        // tag::code[]
        Optional<IncomingAmqpMetadata> metadata = incoming.getMetadata(IncomingAmqpMetadata.class);
        metadata.ifPresent(meta -> {
            String address = meta.getAddress();
            String subject = meta.getSubject();
            boolean durable = meta.isDurable();
            // Use io.vertx.core.json.JsonObject
            JsonObject properties = meta.getProperties();
            // ...
        });
        // end::code[]
    }

}
