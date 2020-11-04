package io.smallrye.reactive.messaging.ack;

import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.DEFAULT_PROCESSING_ACKNOWLEDGMENT_MESSAGE;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.DEFAULT_PROCESSING_ACKNOWLEDGMENT_PAYLOAD;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.MANUAL_ACKNOWLEDGMENT;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.NO_ACKNOWLEDGMENT_MESSAGE;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.NO_ACKNOWLEDGMENT_PAYLOAD;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.POST_PROCESSING_ACKNOWLEDGMENT_MESSAGE;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.POST_PROCESSING_ACKNOWLEDGMENT_PAYLOAD;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.PRE_PROCESSING_ACKNOWLEDGMENT_MESSAGE;
import static io.smallrye.reactive.messaging.ack.SubscriberBeanWithMethodsReturningCompletionStage.PRE_PROCESSING_ACKNOWLEDGMENT_PAYLOAD;

import org.junit.Test;

public class SubscriberWithCompletionStageMethodAcknowledgementTest extends AcknowledgmentTestBase {

    private final Class<SubscriberBeanWithMethodsReturningCompletionStage> beanClass = SubscriberBeanWithMethodsReturningCompletionStage.class;

    @Test
    public void test() {
        SubscriberBeanWithMethodsReturningCompletionStage bean = installInitializeAndGet(beanClass);
        testManual(bean);
        testNoAcknowledgementMessage(bean);
        testNoAcknowledgementPayload(bean);
        testPreProcessingAcknowledgementMessage(bean);
        testPreProcessingAcknowledgementPayload(bean);
        testPostProcessingAcknowledgementMessage(bean);
        testPostProcessingAcknowledgementPayload(bean);
        testDefaultProcessingAcknowledgementMessage(bean);
        testDefaultProcessingAcknowledgementPayload(bean);
    }

    public void testManual(SpiedBeanHelper bean) {
        assertAcknowledgment(bean, MANUAL_ACKNOWLEDGMENT);
    }

    public void testNoAcknowledgementMessage(SpiedBeanHelper bean) {
        assertNoAcknowledgment(bean, NO_ACKNOWLEDGMENT_MESSAGE);
    }

    public void testNoAcknowledgementPayload(SpiedBeanHelper bean) {
        assertNoAcknowledgment(bean, NO_ACKNOWLEDGMENT_PAYLOAD);
    }

    public void testPreProcessingAcknowledgementMessage(SpiedBeanHelper bean) {
        assertPreAcknowledgment(bean, PRE_PROCESSING_ACKNOWLEDGMENT_MESSAGE);
    }

    public void testPreProcessingAcknowledgementPayload(SpiedBeanHelper bean) {
        assertPreAcknowledgment(bean, PRE_PROCESSING_ACKNOWLEDGMENT_PAYLOAD);
    }

    public void testPostProcessingAcknowledgementMessage(SpiedBeanHelper bean) {
        assertPostAcknowledgment(bean, POST_PROCESSING_ACKNOWLEDGMENT_MESSAGE);
    }

    public void testPostProcessingAcknowledgementPayload(SpiedBeanHelper bean) {
        assertPostAcknowledgment(bean, POST_PROCESSING_ACKNOWLEDGMENT_PAYLOAD);
    }

    public void testDefaultProcessingAcknowledgementMessage(SpiedBeanHelper bean) {
        assertPostAcknowledgment(bean, DEFAULT_PROCESSING_ACKNOWLEDGMENT_MESSAGE);
    }

    public void testDefaultProcessingAcknowledgementPayload(SpiedBeanHelper bean) {
        assertPostAcknowledgment(bean, DEFAULT_PROCESSING_ACKNOWLEDGMENT_PAYLOAD);
    }

}
