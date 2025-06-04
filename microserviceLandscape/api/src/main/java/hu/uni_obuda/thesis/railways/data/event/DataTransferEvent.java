package hu.uni_obuda.thesis.railways.data.event;

import java.util.List;

public class DataTransferEvent<T> extends Event<String, T> {

    public enum Type {
        REQUEST,
        DATA_TRANSFER,
        COMPLETE
    }

    private final Type eventType;
    private final List<T> payload;

    public DataTransferEvent() {
        super();
        eventType = null;
        payload = null;
    }

    public DataTransferEvent(Type eventType, String key, List<T> payload) {
        super(key, null);
        this.eventType = eventType;
        this.payload = payload;
    }

    public List<T> getPayload() {
        return payload;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }
}
