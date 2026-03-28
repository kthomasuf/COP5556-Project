import java.util.HashMap;
import java.util.Map;

public class Instance {

    // specific class blueprint/type for this instance
    public ClassSymbol type; 
    
    // store object field and values
    Map<String, Value> memory = new HashMap<>(); 

    public Instance(ClassSymbol type) {
        this.type = type;
    }

    // get field value
    public Value get(String name) {
        if (memory.containsKey(name.toLowerCase())) {
            return memory.get(name.toLowerCase());
        }
        return new Value(0); 
    }

    // store field value
    public void put(String name, Value value) {
        memory.put(name.toLowerCase(), value);
    }
}