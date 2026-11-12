package org.tavall.event;

/**
 * Exception thrown when an event handler fails during dispatch.
 */
public class EventDispatchException extends RuntimeException {

    private final Class<?> payloadType;

    public EventDispatchException(Class<?> payloadType, String message, Throwable cause) {
        super(message, cause);
        this.payloadType = payloadType;
    }

    public Class<?> getPayloadType() {
        return payloadType;
    }
}
