package io.smallrye.reactive.messaging.gcp.pubsub.i18n;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

/**
 * Logging for GCP Pub/Sub Connector
 * Assigned ID range is 14800-14899
 */
@MessageLogger(projectCode = "SRMSG", length = 5)
public interface PubSubLogging extends BasicLogger {

    PubSubLogging log = Logger.getMessageLogger(PubSubLogging.class, "io.smallrye.reactive.messaging.gcp.pubsub");

    @LogMessage(level = Logger.Level.TRACE)
    @Message(id = 14800, value = "Topic %s already exists")
    void topicExistAlready(ProjectTopicName topic, @Cause Throwable t);

    @LogMessage(level = Logger.Level.TRACE)
    @Message(id = 14801, value = "Received pub/sub message %s")
    void receivedMessage(PubsubMessage message);

}
