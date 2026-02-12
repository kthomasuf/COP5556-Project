import java.util.HashMap;
import java.util.Map;
import my.delphi.delphiParser; 

public class ClassSymbol {

    // class name
    String name;
    
    // store fields in class
    Map<String, String> fields = new HashMap<>(); 
    
    // store procedures and functions
    Map<String, delphiParser.ProcedureDeclarationContext> procedures = new HashMap<>();
    Map<String, delphiParser.FunctionDeclarationContext> functions = new HashMap<>();

    // store constructor if it is defined
    delphiParser.ConstructorDeclarationContext constructorImpl;

    public ClassSymbol(String name) {
        this.name = name;
    }
}