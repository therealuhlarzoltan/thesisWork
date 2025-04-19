package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

public class EntityNotFoundException extends RuntimeException {

    private final Object id;
    private final Class<?> type;

    public EntityNotFoundException(Object id, Class<?> type) {
        this.id = id;
        this.type = type;
    }

    public EntityNotFoundException(Object id, Class<?> type, Throwable cause) {
        super(cause);
        this.id = id;
        this.type = type;
    }

    public EntityNotFoundException(Object id, Class<?> type, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(type.getSimpleName() + " with id of " + id + " not found", cause, enableSuppression, writableStackTrace);
        this.id = id;
        this.type = type;
    }

    public Object getId() {
        return id;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String getMessage() {
        return type.getSimpleName() + " with id of " + id + " not found";
    }
}
