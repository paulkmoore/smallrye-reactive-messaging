package skip;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

public class SingleSkip {

    // tag::skip[]

    // Skip when processing payload synchronously - returning `null`
    @Incoming("in")
    @Outgoing("out")
    public String processPayload(String s) {
        if (s.equalsIgnoreCase("skip")) {
            return null;
        }
        return s.toUpperCase();
    }

    // Skip when processing message synchronously - returning `null`
    @Incoming("in")
    @Outgoing("out")
    public Message<String> processMessage(Message<String> m) {
        String s = m.getPayload();
        if (s.equalsIgnoreCase("skip")) {
            return null;
        }
        return m.withPayload(s.toUpperCase());
    }

    // Skip when processing payload asynchronously - returning a `Uni` with a `null` value
    @Incoming("in")
    @Outgoing("out")
    public Uni<String> processPayloadAsync(String s) {
        if (s.equalsIgnoreCase("skip")) {
            // Important, you must not return `null`, but a `null` content
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(s.toUpperCase());
    }

    // Skip when processing message asynchronously - returning a `Uni` with a `null` value
    @Incoming("in")
    @Outgoing("out")
    public Uni<Message<String>> processMessageAsync(Message<String> m) {
        String s = m.getPayload();
        if (s.equalsIgnoreCase("skip")) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(m.withPayload(s.toUpperCase()));
    }

    // end::skip[]

}
