package io.smallrye.reactive.messaging.mqtt;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.reactivex.Flowable;

@ApplicationScoped
public class DynamicTopicProducingBean {

    private final List<String> topics = new ArrayList<>(10);

    @Incoming("dyn-data")
    @Outgoing("sink")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public MqttMessage<String> process(Message<Integer> input) {
        String topic = "T" + input.getPayload();
        topics.add(topic);
        System.out.println("Sending on topic: " + topic);
        return MqttMessage.of(topic, input.getPayload().toString(), MqttQoS.AT_LEAST_ONCE, false).withAck(input::ack);
    }

    @Outgoing("dyn-data")
    public Publisher<Integer> source() {
        return Flowable.range(0, 10);
    }

    public List<String> getTopics() {
        return topics;
    }
}
