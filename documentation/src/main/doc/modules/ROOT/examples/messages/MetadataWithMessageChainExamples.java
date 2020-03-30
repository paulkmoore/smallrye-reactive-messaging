package messages;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class MetadataWithMessageChainExamples {

    // tag::chain[]
    @Outgoing("source")
    public Multi<Message<Integer>> generate() {
        return Multi.createFrom().range(0, 10)
            .map(i -> Message.of(i, Metadata.of(new MyMetadata("author", "me"))));
    }

    @Incoming("source")
    @Outgoing("sink")
    public Message<Integer> increment(Message<Integer> in) {
        return in.withPayload(in.getPayload() + 1);
    }

    @Outgoing("sink")
    public CompletionStage<Void> consume(Message<Integer> in) {
        Optional<MyMetadata> metadata = in.getMetadata(MyMetadata.class);
        return in.ack();
    }
    // end::chain[]

}
