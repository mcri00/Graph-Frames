public class TimestampedElement<T> {
    T element;
    long timestamp;

    public TimestampedElement(T element, long timestamp) {
        this.element = element;
        this.timestamp = timestamp;
    }

}
