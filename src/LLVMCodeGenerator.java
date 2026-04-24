import org.antlr.v4.runtime.*;
import my.delphi.*;
import java.io.IOException;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LLVMCodeGenerator extends delphiBaseVisitor<String> {

    // State
    // Handle scope, doesn't need external class sicne not handling values
    // Store scopes as hashmap, each map stores variable name and LLVM variable name
    private Deque<Map<String, String>> scopeStack = new ArrayDeque<>();

    // store variable name and it's type
    private Map<String, String> varTypes = new HashMap<>();

    // classes temporarily skipped

    // procedures and functions can share these, definitions are essentially the same
    private Map<String, String> funcReturnTypes = new HashMap<>();
    private Map<String, List<String>> funcParamTypes = new HashMap<>();

    // Helper Methods
    private boolean isGlobalScope = true;
    private StringBuilder llvmOutput = new StringBuilder();
    private int operationCounter = 0;

    private String newOperation() {
        return "%" + operationCounter++;
    }

    private void emit(String instruction) {
        llvmOutput.append(instruction).append("\n");
    }

    private void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    private void popScope() {
        scopeStack.pop();
    }

    private void defineVar(String name, String register) {
        scopeStack.peek().put(name, register);
    }

    // lookup variable, get LLVM named representation back
    private String lookupVar(String name) {
        for (Map<String, String> scope : scopeStack) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        throw new RuntimeException("Undefined variable: " + name);
    }

    public void writeToFile(String filename) throws IOException {
        Files.writeString(Path.of(filename), llvmOutput.toString());
    }

    private String toLLVMType(String pascalType) {
        switch (pascalType) {
            case "integer": return "i32";
            case "long": return "i64";
            case "real":    return "double";
            case "boolean": return "i1";
            case "char":    return "i8";
            case "string":  return "i8*";
            default:
                throw new RuntimeException("[LLVM] Unknown Pascal type: " + pascalType);
        }
    }

    @Override
    public String visitProgram(delphiParser.ProgramContext ctx) {
        visit(ctx.block());
        return null;
    }

   @Override
    public String visitBlock(delphiParser.BlockContext ctx) {
        // skipping classes for now
        // for (delphiParser.TypeDefinitionPartContext t : ctx.typeDefinitionPart()) visit(t);
        for (delphiParser.VariableDeclarationPartContext v : ctx.variableDeclarationPart()) visit(v);
        for (delphiParser.ProcedureAndFunctionDeclarationPartContext p : ctx.procedureAndFunctionDeclarationPart()) visit(p);
        emit("define i32 @main() {");
        emit("entry:");
        pushScope();
        visit(ctx.compoundStatement());
        popScope();
        emit("  ret i32 0");
        emit("}");
        return null;
    }

    // declares global variables to have value 0 by default
    @Override
    public String visitVariableDeclaration(delphiParser.VariableDeclarationContext ctx) {
        String pascalType = ctx.type_().getText().toLowerCase();
        String llvmType = toLLVMType(pascalType);
        for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
            String varName = id.getText();

            if (isGlobalScope) {
                emit("@" + varName + " = global " + llvmType + " 0");
                defineVar(varName, "@" + varName);
            }
            else {
                emit("  %" + varName + " = alloca " + llvmType);
                defineVar(varName, "%" + varName);
            }
            varTypes.put(varName, llvmType);
            System.out.println("[LLVM] Declared var: " + varName + " as " + llvmType);
        }

        return null;
    }

    @Override
    public String visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        String varName = ctx.variable().getText().toLowerCase();
        
        // visit the expression to get the register holding the value: ex. %1
        String valueRegister = visit(ctx.expression());
        
        String varLoc = lookupVar(varName);
        String llvmType = varTypes.get(varName);
        
        // check if it's a global variable
        if (varLoc.startsWith("@")) {
            emit("  store " + llvmType + " " + valueRegister + ", " + llvmType + "* " + varLoc);
        } else {
            emit("  store " + llvmType + " " + valueRegister + ", " + llvmType + "* " + varLoc);
        }
        
        System.out.println("[LLVM] Assignment: " + varName + " = " + valueReg);
        return null;
    }

    @Override
    public String visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
        visit(ctx.statements());
        return null;
    }

    @Override
    public String visitStatements(delphiParser.StatementsContext ctx) {
        for (delphiParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }
}