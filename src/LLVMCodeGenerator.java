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
    // store temporary register types
    private Map<String, String> tempTypes = new HashMap<>();

    // classes temporarily skipped

    // procedures and functions can share these, definitions are essentially the same
    private Map<String, String> funcReturnTypes = new HashMap<>();
    private Map<String, List<String>> funcParamTypes = new HashMap<>();

    // Helper Methods
    private boolean isGlobalScope = true;
    private StringBuilder llvmOutput = new StringBuilder();
    private int tempCounter = 0;

    private String newTemp() {
        return "%" + tempCounter++;
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

    private String getType(String reg) {
        if (tempTypes.containsKey(reg)) return tempTypes.get(reg);
        String name = reg.replace("%", "").replace("@", "");
        if (varTypes.containsKey(name)) return varTypes.get(name);
        if (reg.contains(".")) return "float";
        return "i32";
    }

    public void writeToFile(String filename) throws IOException {
        Files.writeString(Path.of(filename), llvmOutput.toString());
    }

    private String toLLVMType(String pascalType) {
        switch (pascalType) {
            case "integer": return "i32";
            case "long": return "i64";
            case "real":    return "float";
            case "boolean": return "i1";
            case "char":    return "i8";
            case "string":  return "i8*";
            default:
                throw new RuntimeException("[LLVM] Unknown Pascal type: " + pascalType);
        }
    }

    @Override
    public String visitProgram(delphiParser.ProgramContext ctx) {
        pushScope();
        visit(ctx.block());
        popScope();
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
        
        System.out.println("[LLVM] Assignment: " + varName + " = " + valueRegister);
        return null;
    }

    @Override
    public String visitVariable(delphiParser.VariableContext ctx) {
        String varName = ctx.getText().toLowerCase();
        String varLoc = lookupVar(varName);
        String type = varTypes.get(varName);
        String result = newTemp();

        emit("  " + result + " = load " + type + ", " + type + "* " + varLoc);

        tempTypes.put(result, type);

        System.out.println("[LLVM] Load var: " + varName + " into " + result);
        return result;
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

    // relational operator support
    @Override
    public String visitExpression(delphiParser.ExpressionContext ctx) {
        String left = visit(ctx.simpleExpression());

        if (ctx.relationaloperator() != null) {
            String right = visit(ctx.expression());
            String operator = ctx.relationaloperator().getText().toUpperCase();
            String result = newTemp();

            String leftType = getType(left);
            String rightType = getType(right);

            if (leftType.equals("float") || rightType.equals("float")) {
                switch (operator) {
                    case "=": emit("  " + result + " = fcmp oeq float " + left + ", " + right); break;
                    case "<>": emit("  " + result + " = fcmp one float " + left + ", " + right); break;
                    case "<": emit("  " + result + " = fcmp olt float " + left + ", " + right); break;
                    case "<=": emit("  " + result + " = fcmp ole float " + left + ", " + right); break;
                    case ">": emit("  " + result + " = fcmp ogt float " + left + ", " + right); break;
                    case ">=": emit("  " + result + " = fcmp oge float " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else if (leftType.equals("i1") || rightType.equals("i1")) {
                switch (operator) {
                    case "=": emit("  " + result + " = icmp eq i1 " + left + ", " + right); break;
                    case "<>": emit("  " + result + " = icmp ne i1 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else {
                switch (operator) {
                    case "=": emit("  " + result + " = icmp eq i32 " + left + ", " + right); break;
                    case "<>": emit("  " + result + " = icmp ne i32 " + left + ", " + right); break;
                    case "<": emit("  " + result + " = icmp slt i32 " + left + ", " + right); break;
                    case "<=": emit("  " + result + " = icmp sle i32 " + left + ", " + right); break;
                    case ">": emit("  " + result + " = icmp sgt i32 " + left + ", " + right); break;
                    case ">=": emit("  " + result + " = icmp sge i32 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            tempTypes.put(result, "i1");
            return result;
        }
        return left;
    }
    
    // basic math support, need to add more operators and precedence handling 
    @Override
    public String visitSimpleExpression(delphiParser.SimpleExpressionContext ctx) {
        String left = visit(ctx.term());

        if (ctx.additiveoperator() != null) {
            String right = visit(ctx.simpleExpression());
            String operator = ctx.additiveoperator().getText().toUpperCase();
            String result = newTemp();

            String leftType = getType(left);
            String rightType = getType(right);

            String resultType = (leftType.equals("float") || rightType.equals("float")) ? "float" : "i32";

            if (leftType.equals("float") || rightType.equals("float")) {
                switch (operator) {
                    case "+": emit("  " + result + " = fadd float " + left + ", " + right); break;
                    case "-": emit("  " + result + " = fsub float " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else if (operator.equals("OR")) {
                if (leftType.equals("i1") || rightType.equals("i1")) {
                    emit("  " + result + " = or i1 " + left + ", " + right);
                    resultType = "i1";
                } else {
                    emit("  " + result + " = or i32 " + left + ", " + right);
                    resultType = "i32";
                }
            }
            else {
                switch (operator) {
                    case "+": emit("  " + result + " = add i32 " + left + ", " + right); break;
                    case "-": emit("  " + result + " = sub i32 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }  
            }
            tempTypes.put(result, resultType);
            return result;
        }
        return left;
    }

    // multiplicative math support
    @Override
    public String visitTerm(delphiParser.TermContext ctx) {
        String left = visit(ctx.signedFactor());

        if (ctx.multiplicativeoperator() != null) {
            String right = visit(ctx.term());
            String operator = ctx.multiplicativeoperator().getText().toUpperCase();
            String result = newTemp();

            String leftType = getType(left);
            String rightType = getType(right);

            String resultType = (leftType.equals("float") || rightType.equals("float")) ? "float" : "i32";

            if (leftType.equals("float") || rightType.equals("float")) {
                switch (operator) {
                    case "*": emit("  " + result + " = fmul float " + left + ", " + right); break;
                    case "/": emit("  " + result + " = fdiv float " + left + ", " + right); break;
                    case "DIV": emit("  " + result + " = fdiv float " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else if (leftType.equals("i1") || rightType.equals("i1")) {
                switch (operator) {
                    case "AND": emit("  " + result + " = and i32 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else {
                switch (operator) {
                    case "*": emit("  " + result + " = mul i32 " + left + ", " + right); break;
                    case "/": emit("  " + result + " = sdiv i32 " + left + ", " + right); break;
                    case "DIV": emit("  " + result + " = sdiv i32 " + left + ", " + right); break;
                    case "MOD": emit("  " + result + " = srem i32 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            tempTypes.put(result, resultType);
            return result;
        }
        return left;
    }

    // reading values
    @Override
    public String visitFactor(delphiParser.FactorContext ctx) {
        if (ctx.bool_() != null) {
            String result = newTemp();
            String val = ctx.bool_().TRUE() != null ? "1" : "0";
            emit("  " + result + " = add i1 0, " + val);
            tempTypes.put(result, "i1");
            return result;
        }
        
        if (ctx.NOT() != null) {
            String val = visit(ctx.factor());
            String result = newTemp();
            String type = getType(val);
            if (type.equals("i1")) {
                // boolean NOT
                emit("  " + result + " = xor i1 " + val + ", 1");
                tempTypes.put(result, "i1");
            }
            else {
                // bitwise NOT on integer
                emit("  " + result + " = xor i32 " + val + ", -1");
                tempTypes.put(result, "i32");
            }
            return result;
        }
        
        if (ctx.functionDesignator() != null) return visit(ctx.functionDesignator()); 

        if (ctx.LPAREN() != null) return visit(ctx.expression());

        if (ctx.unsignedConstant() != null) {
            String text = ctx.unsignedConstant().getText();
            if (text.startsWith("'")) {
                // handle strings later
                return text;
            }
            if (text.contains(".") || text.toLowerCase().contains("e")) {
                tempTypes.put(text, "float");
                return text;
            }
            return text;
        }

        if (ctx.variable() != null) {
            return visit(ctx.variable());

            // add class/function logic again later
        }
        return "0";
    }
}