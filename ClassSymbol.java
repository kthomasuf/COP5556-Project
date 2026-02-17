import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import my.delphi.delphiParser; 

public class ClassSymbol {

    // class name
    String name;

    // track which members are private
    Set<String> privateMembers = new HashSet<>();
    
    // store fields in class
    Map<String, String> fields = new HashMap<>(); 
    
    // store procedures and functions
    Map<String, delphiParser.ProcedureDeclarationContext> procedures = new HashMap<>();
    Map<String, delphiParser.FunctionDeclarationContext> functions = new HashMap<>();

    // store constructor if it is defined
    delphiParser.ConstructorDeclarationContext constructorImpl;
    delphiParser.DestructorDeclarationContext destructorImpl;

    public ClassSymbol(String name) {
        this.name = name;
    }

    // helper to check visibility
    public boolean isPrivate(String memberName) {
        return privateMembers.contains(memberName.toLowerCase());
    }
}


