public class Value {

    // values for no result 
    public static Value NULL = new Value(new Object());
    public static Value VOID = new Value(new Object());

    // raw data
    final Object value;
    
    // raw data packaged
    public Value(Object value) {
        this.value = value;
    }

    // assume data is is integer but may crash with other data
    public Integer asInt() {
        return (Integer) value;
    }

    // string rep for pascal
    public String asString() {
        return String.valueOf(value);
    }

    // if this value is an instance
    public Instance asInstance() {
        if (value instanceof Instance) {
            return (Instance) value;
        }
        return null;
    }

    // for java
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}