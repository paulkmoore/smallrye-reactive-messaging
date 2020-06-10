package io.smallrye.reactive.messaging;

import static io.smallrye.reactive.messaging.i18n.ProviderMessages.msg;

public class ProcessingException extends RuntimeException {
    public ProcessingException(String method, Throwable cause) {
        super(msg.methodCallingExceptionMessage(method), cause);
    }
}
