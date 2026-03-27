import java.util.HashMap;
import java.util.Map;
import my.delphi.delphiParser;

public class Environment {

    // parent scope
    Environment enclosing; 

    // storage for variables, classes, and global procedures
    Map<String, Value> values = new HashMap<>();
    Map<String, ClassSymbol> classes = new HashMap<>();
    // Map<String, delphiParser.ProcedureDeclarationContext> procedures = new HashMap<>();
    Map<String, ProcedureSymbol> procedures = new HashMap<>();
    Map<String, delphiParser.FunctionDeclarationContext> functions = new HashMap<>();
    // Map<String, FunctionSymbol> functions = new HashMap<>();

    // global environemt
    public Environment() {
        this.enclosing = null;
    }

    // nested environment like inside a function or so
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // check if variable exists in current scope or higher scopes
    public boolean isDefined(String name) {
        if (values.containsKey(name.toLowerCase())) return true;
        if (enclosing != null) return enclosing.isDefined(name);
        return false;
    }

    // needed to handle function return assignments properly
    public boolean isDefinedLocal(String name) {
        return values.containsKey(name.toLowerCase());
    }

    // define variable in current scope
    public void define(String name, Value value) {
        values.put(name.toLowerCase(), value); 
    }

    // get variable value
    public Value get(String name) {
        if (values.containsKey(name.toLowerCase())) return values.get(name.toLowerCase());
        if (enclosing != null) return enclosing.get(name);
        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    // update existing variable 
    public void assign(String name, Value value) {
        if (values.containsKey(name.toLowerCase())) {
            values.put(name.toLowerCase(), value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    // register new class type
    public void defineClass(String name, ClassSymbol symbol) {
        classes.put(name.toLowerCase(), symbol);
    }

    // get class blueprint
    public ClassSymbol getClass(String name) {
        if (classes.containsKey(name.toLowerCase())) return classes.get(name.toLowerCase());
        if (enclosing != null) return enclosing.getClass(name);
        return null;
    }

    // register global procedure
    // public void defineProcedure(String name, delphiParser.ProcedureDeclarationContext ctx) {
    //     procedures.put(name.toLowerCase(), ctx);
    // }
    public void defineProcedure(String name, ProcedureSymbol symbol) {
        procedures.put(name.toLowerCase(), symbol);
    }

    // get global procedure body
    // public delphiParser.ProcedureDeclarationContext getProcedure(String name) {
    //     if (procedures.containsKey(name.toLowerCase())) return procedures.get(name.toLowerCase());
    //     if (enclosing != null) return enclosing.getProcedure(name);
    //     return null;
    // }
    public ProcedureSymbol getProcedure(String name) {
        if (procedures.containsKey(name.toLowerCase())) return procedures.get(name.toLowerCase());
        if (enclosing != null) return enclosing.getProcedure(name);
        return null;
    }

    // register global function
    public void defineFunction(String name, delphiParser.FunctionDeclarationContext ctx) {
        functions.put(name.toLowerCase(), ctx);
    }
    // public void defineFunction(String name, FunctionSymbol symbol) {
    //     functions.put(name.toLowerCase(), symbol);
    // }

    // get global function body
    public delphiParser.FunctionDeclarationContext getFunction(String name) {
        if (functions.containsKey(name.toLowerCase())) return functions.get(name.toLowerCase());
        if (enclosing != null) return enclosing.getFunction(name);
        return null;
    }
    // public FunctionSymbol getFunction(String name) {
    //     if (functions.containsKey(name.toLowerCase())) return functions.get(name.toLowerCase());
    //     if (enclosing != null) return enclosing.getFunction(name);
    //     return null;
    // }
}