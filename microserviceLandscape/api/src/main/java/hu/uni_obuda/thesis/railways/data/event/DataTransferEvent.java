package hu.uni_obuda.thesis.railways.data.event;

import java.util.List;

public class DataTransferEvent<T> extends Event<String, T> {

    public enum Type {
        REQUEST,
        DATA_TRANSFER,
        COMPLETE
    }

    private final Type eventType;

    public DataTransferEvent() {
        super();
        eventType = null;
    }

    public DataTransferEvent(Type eventType, String key, T data) {
        super(key, data);
        this.eventType = eventType;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }
}
