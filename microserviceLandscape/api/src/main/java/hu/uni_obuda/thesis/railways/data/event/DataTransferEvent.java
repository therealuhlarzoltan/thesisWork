package hu.uni_obuda.thesis.railways.data.event;

import java.util.List;

public class DataTransferEvent<T> extends Event<String, T> {

    public enum Type {
        REQUEST,
        DATA_TRANSFER,
        COMPLETE
    }

    private final Type type;
    private final List<T> data;

    public DataTransferEvent(Type type, String key, List<T> data) {
        super(key, null);
        this.type = type;
        this.data = data;
    }

    public List<T> getPayload() {
        return data;
    }

    @Override
    public Type getEventType() {
        return type;
    }

    @Override
    public T getData() {
        throw new UnsupportedOperationException();
    }
}
