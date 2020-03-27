package io.smallrye.reactive.messaging.kafka;

import io.reactivex.Flowable;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.eclipse.microprofile.reactive.messaging.*;
import org.reactivestreams.Publisher;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ProducingMessageWithHeaderBean {

    private AtomicInteger counter = new AtomicInteger();

    @Incoming("data")
    @Outgoing("output-2")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public Message<Integer> process(Message<Integer> input) {
        List<RecordHeader> list = Arrays.asList(
                new RecordHeader("hello", "clement".getBytes()),
                new RecordHeader("count", Integer.toString(counter.incrementAndGet()).getBytes()));
        return Message.of(
                input.getPayload() + 1,
                Metadata.of(OutgoingKafkaRecordMetadata.builder().withKey(Integer.toString(input.getPayload()))
                        .withHeaders(list).build()),
                input::ack);
    }

    @Outgoing("data")
    public Publisher<Integer> source() {
        return Flowable.range(0, 10);
    }

}
