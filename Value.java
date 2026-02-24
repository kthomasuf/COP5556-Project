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

    public boolean isInt() {
        return value instanceof Integer;
    }

    public boolean isDouble() {
        return value instanceof Double;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public boolean isInstance() {
        return value instanceof Instance;
    }

    // 
    public Integer asInt() {
        return (Integer) value;
    }

    public Double asDouble() {
        if (isInt()) {
            return ((Integer) value).doubleValue();
        }
        return (Double) value;
    }

    // string rep for pascal
    public String asString() {
        return String.valueOf(value);
    }

    public Boolean asBoolean() {
        return (Boolean) value;
    }

    // if this value is an instance
    public Instance asInstance() {
        if (isInstance()) {
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