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

    private static class FieldInfo {
        String name;
        String llvmType;
        String visibility;
        String declaringClass;
        int index;
    }

    private static class ClassInfo {
        String name;
        String parentName;
        List<FieldInfo> fields = new ArrayList<>();
        Map<String, FieldInfo> fieldsByName = new HashMap<>();
        boolean layoutComplete = false;
    }

    private static class LValue {
        String pointer;
        String type;
    }

    // State
    // Handle scope, doesn't need external class sicne not handling values
    // Store scopes as hashmap, each map stores variable name and LLVM variable name
    private Deque<Map<String, String>> scopeStack = new ArrayDeque<>();

    // store variable name and it's type
    private Map<String, String> varTypes = new HashMap<>();
    private Map<String, String> varClassTypes = new HashMap<>();
    // store temporary register types
    private Map<String, String> tempTypes = new HashMap<>();
    private Map<String, ClassInfo> classes = new HashMap<>();

    // classes temporarily skipped

    // procedures and functions can share these, definitions are essentially the same
    private Map<String, String> funcReturnTypes = new HashMap<>();
    private Map<String, List<String>> funcParamTypes = new HashMap<>();

    // Helper Methods
    private boolean isGlobalScope = true;
    private StringBuilder llvmOutput = new StringBuilder();
    private int tempCounter = 0;
    private int labelCounter = 0;
    private boolean blockTerminated = false;
    private Deque<String> breakLabels = new ArrayDeque<>();
    private Deque<String> continueLabels = new ArrayDeque<>();
    private String currentClass = null;

    private String newTemp() {
        return "%t" + tempCounter++;
    }

    private String newLabel(String prefix) {
        return prefix + "." + labelCounter++;
    }

    private void emit(String instruction) {
        llvmOutput.append(instruction).append("\n");
    }

    private void emitLabel(String label) {
        llvmOutput.append(label).append(":\n");
        blockTerminated = false;
    }

    private void emitBranch(String label) {
        if (!blockTerminated) {
            emit("  br label %" + label);
            blockTerminated = true;
        }
    }

    private void emitConditionalBranch(String condition, String trueLabel, String falseLabel) {
        emit("  br i1 " + condition + ", label %" + trueLabel + ", label %" + falseLabel);
        blockTerminated = true;
    }

    private void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    private void popScope() {
        scopeStack.pop();
    }

    private void defineVar(String name, String register) {
        scopeStack.peek().put(name.toLowerCase(), register);
    }

    // lookup variable, get LLVM named representation back
    private String lookupVar(String name) {
        String lookup = name.toLowerCase();
        for (Map<String, String> scope : scopeStack) {
            if (scope.containsKey(lookup)) return scope.get(lookup);
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

    private void setType(String reg, String type) {
        tempTypes.put(reg, type);
    }

    private String castTo(String value, String fromType, String toType) {
        if (fromType.equals(toType)) return value;

        String result;
        if (fromType.equals("i32") && toType.equals("float")) {
            result = newTemp();
            emit("  " + result + " = sitofp i32 " + value + " to float");
        } else if (fromType.equals("i1") && toType.equals("i32")) {
            result = newTemp();
            emit("  " + result + " = zext i1 " + value + " to i32");
        } else if (fromType.equals("i32") && toType.equals("i1")) {
            result = newTemp();
            emit("  " + result + " = icmp ne i32 " + value + ", 0");
        } else if (fromType.equals("i1") && toType.equals("float")) {
            String widened = castTo(value, "i1", "i32");
            result = newTemp();
            emit("  " + result + " = sitofp i32 " + widened + " to float");
        } else if (fromType.equals("float") && toType.equals("i32")) {
            result = newTemp();
            emit("  " + result + " = fptosi float " + value + " to i32");
        } else if (fromType.equals("float") && toType.equals("i1")) {
            result = newTemp();
            emit("  " + result + " = fcmp one float " + value + ", 0.0");
        } else {
            throw new RuntimeException("[LLVM] Unsupported cast from " + fromType + " to " + toType);
        }

        setType(result, toType);
        return result;
    }

    private String defaultValue(String llvmType) {
        if (llvmType.endsWith("*")) return "null";
        if (llvmType.equals("float")) return "0.0";
        return "0";
    }

    private String sizeOfType(String llvmType) {
        if (llvmType.equals("i1") || llvmType.equals("i8")) return "1";
        if (llvmType.equals("i32") || llvmType.equals("float")) return "4";
        if (llvmType.equals("i64") || llvmType.endsWith("*")) return "8";
        throw new RuntimeException("[LLVM] Cannot size type: " + llvmType);
    }

    private String classSize(ClassInfo info) {
        int total = 0;
        for (FieldInfo field : info.fields) {
            total += Integer.parseInt(sizeOfType(field.llvmType));
        }
        return Integer.toString(Math.max(total, 1));
    }

    private void emitRuntimeDeclarations() {
        // using the c runtime for simple native io and heap allocation
        emit("declare i32 @printf(i8*, ...)");
        emit("declare i32 @scanf(i8*, ...)");
        emit("declare i8* @malloc(i64)");
        emit("declare void @free(i8*)");
        emit("@.fmt.int = private constant [4 x i8] c\"%d\\0A\\00\"");
        emit("@.fmt.float = private constant [4 x i8] c\"%f\\0A\\00\"");
        emit("@.scan.int = private constant [3 x i8] c\"%d\\00\"");
        emit("@.scan.float = private constant [3 x i8] c\"%f\\00\"");
    }

    public void writeToFile(String filename) throws IOException {
        Files.writeString(Path.of(filename), llvmOutput.toString());
    }

    private String toLLVMType(String pascalType) {
        String type = pascalType.toLowerCase();
        if (classes.containsKey(type)) return "%" + classes.get(type).name + "*";

        switch (type) {
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
        emitRuntimeDeclarations();
        visit(ctx.block());
        popScope();
        return null;
    }

   @Override
    public String visitBlock(delphiParser.BlockContext ctx) {
        // skipping classes for now
        // class metadata has to be available before vars are lowered
        for (delphiParser.TypeDefinitionPartContext t : ctx.typeDefinitionPart()) visit(t);
        completeClassLayouts();
        emitClassTypes();
        for (delphiParser.VariableDeclarationPartContext v : ctx.variableDeclarationPart()) visit(v);
        for (delphiParser.ProcedureAndFunctionDeclarationPartContext p : ctx.procedureAndFunctionDeclarationPart()) visit(p);
        emit("define i32 @main() {");
        emit("entry:");
        pushScope();
        visit(ctx.compoundStatement());
        popScope();
        if (!blockTerminated) emit("  ret i32 0");
        emit("}");
        return null;
    }

    @Override
    public String visitTypeDefinition(delphiParser.TypeDefinitionContext ctx) {
        if (ctx.type_() == null ||
            ctx.type_().structuredType() == null ||
            ctx.type_().structuredType().unpackedStructuredType() == null ||
            ctx.type_().structuredType().unpackedStructuredType().classType() == null) {
            return null;
        }

        String className = ctx.identifier().getText();
        ClassInfo info = new ClassInfo();
        info.name = className;

        delphiParser.ClassTypeContext classCtx = ctx.type_().structuredType().unpackedStructuredType().classType();
        if (classCtx.classParent() != null) {
            info.parentName = classCtx.classParent().identifier().getText().toLowerCase();
        }

        if (classCtx.classBody() != null) {
            for (delphiParser.ClassSectionContext section : classCtx.classBody().classSection()) {
                String visibility = section.visibilitySpecifier().PRIVATE() != null ? "private" : "public";
                if (section.classMemberList() == null) continue;

                for (delphiParser.ClassMemberContext member : section.classMemberList().classMember()) {
                    if (member.fieldDeclaration() == null) continue;

                    String llvmType = toLLVMType(member.fieldDeclaration().type_().getText());
                    for (delphiParser.IdentifierContext id : member.fieldDeclaration().identifierList().identifier()) {
                        FieldInfo field = new FieldInfo();
                        field.name = id.getText();
                        field.llvmType = llvmType;
                        field.visibility = visibility;
                        field.declaringClass = className.toLowerCase();
                        info.fields.add(field);
                    }
                }
            }
        }

        classes.put(className.toLowerCase(), info);
        return null;
    }

    private void completeClassLayouts() {
        for (ClassInfo info : classes.values()) {
            completeClassLayout(info);
        }
    }

    private void completeClassLayout(ClassInfo info) {
        if (info.layoutComplete) return;

        List<FieldInfo> ownFields = new ArrayList<>(info.fields);
        info.fields.clear();
        info.fieldsByName.clear();

        if (info.parentName != null) {
            ClassInfo parent = classes.get(info.parentName);
            if (parent == null) throw new RuntimeException("[LLVM] Unknown parent class: " + info.parentName);
            completeClassLayout(parent);
            info.fields.addAll(parent.fields);
        }

        info.fields.addAll(ownFields);
        for (int i = 0; i < info.fields.size(); i++) {
            FieldInfo field = info.fields.get(i);
            field.index = i;
            info.fieldsByName.put(field.name.toLowerCase(), field);
        }
        info.layoutComplete = true;
    }

    private void emitClassTypes() {
        for (ClassInfo info : classes.values()) {
            List<String> fieldTypes = new ArrayList<>();
            for (FieldInfo field : info.fields) fieldTypes.add(field.llvmType);
            emit("%" + info.name + " = type { " + String.join(", ", fieldTypes) + " }");
        }
    }

    // declares global variables to have value 0 by default
    @Override
    public String visitVariableDeclaration(delphiParser.VariableDeclarationContext ctx) {
        String pascalType = ctx.type_().getText().toLowerCase();
        String llvmType = toLLVMType(pascalType);
        String classType = classes.containsKey(pascalType) ? pascalType : null;
        for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
            String varName = id.getText();

            if (isGlobalScope) {
                emit("@" + varName + " = global " + llvmType + " " + defaultValue(llvmType));
                defineVar(varName, "@" + varName);
            }
            else {
                emit("  %" + varName + " = alloca " + llvmType);
                defineVar(varName, "%" + varName);
            }
            varTypes.put(varName.toLowerCase(), llvmType);
            if (classType != null) varClassTypes.put(varName.toLowerCase(), classType);
            System.out.println("[LLVM] Declared var: " + varName + " as " + llvmType);
        }

        return null;
    }

    @Override
    public String visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
        String varName = ctx.variable().getText().toLowerCase();
        
        // visit the expression to get the register holding the value: ex. %1
        String valueRegister = visit(ctx.expression());
        
        LValue target = getLValue(ctx.variable());
        String llvmType = target.type;
        valueRegister = castTo(valueRegister, getType(valueRegister), llvmType);
        
        // check if it's a global variable
        if (target.pointer.startsWith("@")) {
            emit("  store " + llvmType + " " + valueRegister + ", " + llvmType + "* " + target.pointer);
        } else {
            emit("  store " + llvmType + " " + valueRegister + ", " + llvmType + "* " + target.pointer);
        }
        
        System.out.println("[LLVM] Assignment: " + varName + " = " + valueRegister);
        return null;
    }

    @Override
    public String visitVariable(delphiParser.VariableContext ctx) {
        LValue lvalue = getLValue(ctx);
        String result = newTemp();

        emit("  " + result + " = load " + lvalue.type + ", " + lvalue.type + "* " + lvalue.pointer);

        tempTypes.put(result, lvalue.type);

        System.out.println("[LLVM] Load var: " + ctx.getText().toLowerCase() + " into " + result);
        return result;
    }

    private LValue getLValue(delphiParser.VariableContext ctx) {
        String text = ctx.getText();
        if (text.contains(".")) {
            return getFieldLValue(text);
        }

        String varName = text.toLowerCase();
        LValue out = new LValue();
        out.pointer = lookupVar(varName);
        out.type = varTypes.get(varName);
        if (out.type == null) throw new RuntimeException("[LLVM] Unknown variable type: " + text);
        return out;
    }

    private LValue getFieldLValue(String text) {
        String[] parts = text.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("[LLVM] Only simple obj.field access is supported: " + text);
        }

        String objectName = parts[0].toLowerCase();
        String fieldName = parts[1].toLowerCase();
        String className = varClassTypes.get(objectName);
        if (className == null) throw new RuntimeException("[LLVM] " + parts[0] + " is not a class object");

        ClassInfo info = classes.get(className);
        FieldInfo field = info.fieldsByName.get(fieldName);
        if (field == null) throw new RuntimeException("[LLVM] Unknown field: " + parts[1]);
        checkFieldAccess(field, info.name);

        String objectPointer = visitPlainVariable(objectName);
        String fieldPointer = newTemp();
        emit("  " + fieldPointer + " = getelementptr %" + info.name + ", %" + info.name + "* " +
             objectPointer + ", i32 0, i32 " + field.index);
        setType(fieldPointer, field.llvmType + "*");

        LValue out = new LValue();
        out.pointer = fieldPointer;
        out.type = field.llvmType;
        return out;
    }

    private String visitPlainVariable(String varName) {
        String varLoc = lookupVar(varName);
        String type = varTypes.get(varName.toLowerCase());
        String result = newTemp();
        emit("  " + result + " = load " + type + ", " + type + "* " + varLoc);
        setType(result, type);
        return result;
    }

    private void checkFieldAccess(FieldInfo field, String targetClassName) {
        if (!field.visibility.equals("private")) return;
        if (currentClass != null && currentClass.equals(field.declaringClass)) return;
        throw new RuntimeException("[LLVM] Access denied: private field '" + field.name + "' on " + targetClassName);
    }

    @Override
    public String visitObjectInstantiation(delphiParser.ObjectInstantiationContext ctx) {
        String className = ctx.identifier(0).getText();
        ClassInfo info = classes.get(className.toLowerCase());
        if (info == null) throw new RuntimeException("[LLVM] Unknown class: " + className);

        String raw = newTemp();
        String object = newTemp();
        emit("  " + raw + " = call i8* @malloc(i64 " + classSize(info) + ")");
        emit("  " + object + " = bitcast i8* " + raw + " to %" + info.name + "*");
        setType(raw, "i8*");
        setType(object, "%" + info.name + "*");
        return object;
    }

    @Override
    public String visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        String callName = ctx.variable().getText();
        if (callName.equalsIgnoreCase("WriteLn")) {
            emitWriteLn(ctx.parameterList());
            return null;
        }
        if (callName.equalsIgnoreCase("ReadLn")) {
            emitReadLn(ctx.parameterList());
            return null;
        }
        return super.visitProcedureStatement(ctx);
    }

    private void emitWriteLn(delphiParser.ParameterListContext params) {
        if (params == null || params.actualParameter().isEmpty()) return;

        String value = visit(params.actualParameter(0).expression());
        String type = getType(value);
        if (type.equals("i1")) {
            value = castTo(value, "i1", "i32");
            type = "i32";
        }

        if (type.equals("float")) {
            String asDouble = newTemp();
            emit("  " + asDouble + " = fpext float " + value + " to double");
            setType(asDouble, "double");
            emit("  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.float, i32 0, i32 0), double " + asDouble + ")");
        } else {
            emit("  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 " + value + ")");
        }
    }

    private void emitReadLn(delphiParser.ParameterListContext params) {
        if (params == null || params.actualParameter().isEmpty()) {
            throw new RuntimeException("[LLVM] ReadLn expects a variable");
        }

        delphiParser.ExpressionContext expr = params.actualParameter(0).expression();
        if (expr.simpleExpression().term().signedFactor().factor().variable() == null) {
            throw new RuntimeException("[LLVM] ReadLn target must be a variable");
        }

        LValue target = getLValue(expr.simpleExpression().term().signedFactor().factor().variable());
        if (target.type.equals("float")) {
            emit("  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.scan.float, i32 0, i32 0), float* " + target.pointer + ")");
        } else if (target.type.equals("i32")) {
            emit("  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.scan.int, i32 0, i32 0), i32* " + target.pointer + ")");
        } else if (target.type.equals("i1")) {
            String tmp = newTemp();
            String loaded = newTemp();
            String boolValue = newTemp();
            emit("  " + tmp + " = alloca i32");
            emit("  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.scan.int, i32 0, i32 0), i32* " + tmp + ")");
            emit("  " + loaded + " = load i32, i32* " + tmp);
            emit("  " + boolValue + " = icmp ne i32 " + loaded + ", 0");
            emit("  store i1 " + boolValue + ", i1* " + target.pointer);
            setType(tmp, "i32*");
            setType(loaded, "i32");
            setType(boolValue, "i1");
        } else {
            throw new RuntimeException("[LLVM] ReadLn does not support type: " + target.type);
        }
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

    private String conditionToI1(String value) {
        String type = getType(value);
        return castTo(value, type, "i1");
    }

    @Override
    public String visitBreakStatement(delphiParser.BreakStatementContext ctx) {
        if (breakLabels.isEmpty()) throw new RuntimeException("[LLVM] BREAK used outside loop");
        emitBranch(breakLabels.peek());
        return null;
    }

    @Override
    public String visitContinueStatement(delphiParser.ContinueStatementContext ctx) {
        if (continueLabels.isEmpty()) throw new RuntimeException("[LLVM] CONTINUE used outside loop");
        emitBranch(continueLabels.peek());
        return null;
    }

    @Override
    public String visitIfStatement(delphiParser.IfStatementContext ctx) {
        String thenLabel = newLabel("if.then");
        String elseLabel = ctx.ELSE() != null ? newLabel("if.else") : null;
        String endLabel = newLabel("if.end");

        String condition = conditionToI1(visit(ctx.expression()));
        emitConditionalBranch(condition, thenLabel, elseLabel != null ? elseLabel : endLabel);

        emitLabel(thenLabel);
        visit(ctx.statement(0));
        emitBranch(endLabel);

        if (elseLabel != null) {
            emitLabel(elseLabel);
            visit(ctx.statement(1));
            emitBranch(endLabel);
        }

        emitLabel(endLabel);
        return null;
    }

    @Override
    public String visitWhileStatement(delphiParser.WhileStatementContext ctx) {
        String condLabel = newLabel("while.cond");
        String bodyLabel = newLabel("while.body");
        String endLabel = newLabel("while.end");

        emitBranch(condLabel);

        emitLabel(condLabel);
        String condition = conditionToI1(visit(ctx.expression()));
        emitConditionalBranch(condition, bodyLabel, endLabel);

        breakLabels.push(endLabel);
        continueLabels.push(condLabel);

        emitLabel(bodyLabel);
        visit(ctx.statement());
        emitBranch(condLabel);

        continueLabels.pop();
        breakLabels.pop();

        emitLabel(endLabel);
        return null;
    }

    @Override
    public String visitForStatement(delphiParser.ForStatementContext ctx) {
        String varName = ctx.identifier().getText().toLowerCase();
        String varLoc = lookupVar(varName);
        String varType = varTypes.get(varName);
        if (varType == null) throw new RuntimeException("[LLVM] Unknown for-loop variable: " + varName);
        if (!varType.equals("i32")) throw new RuntimeException("[LLVM] FOR loop variable must be integer");

        String start = visit(ctx.forList().initialValue().expression());
        start = castTo(start, getType(start), "i32");
        String end = visit(ctx.forList().finalValue().expression());
        end = castTo(end, getType(end), "i32");
        boolean isTo = ctx.forList().TO() != null;

        String condLabel = newLabel("for.cond");
        String bodyLabel = newLabel("for.body");
        String updateLabel = newLabel("for.update");
        String endLabel = newLabel("for.end");

        emit("  store i32 " + start + ", i32* " + varLoc);
        emitBranch(condLabel);

        emitLabel(condLabel);
        String current = newTemp();
        String compare = newTemp();
        emit("  " + current + " = load i32, i32* " + varLoc);
        if (isTo) {
            emit("  " + compare + " = icmp sle i32 " + current + ", " + end);
        } else {
            emit("  " + compare + " = icmp sge i32 " + current + ", " + end);
        }
        setType(current, "i32");
        setType(compare, "i1");
        emitConditionalBranch(compare, bodyLabel, endLabel);

        breakLabels.push(endLabel);
        continueLabels.push(updateLabel);

        emitLabel(bodyLabel);
        visit(ctx.statement());
        emitBranch(updateLabel);

        emitLabel(updateLabel);
        String updateCurrent = newTemp();
        String next = newTemp();
        emit("  " + updateCurrent + " = load i32, i32* " + varLoc);
        if (isTo) {
            emit("  " + next + " = add i32 " + updateCurrent + ", 1");
        } else {
            emit("  " + next + " = sub i32 " + updateCurrent + ", 1");
        }
        emit("  store i32 " + next + ", i32* " + varLoc);
        setType(updateCurrent, "i32");
        setType(next, "i32");
        emitBranch(condLabel);

        continueLabels.pop();
        breakLabels.pop();

        emitLabel(endLabel);
        return null;
    }

    // relational operator support
    @Override
    public String visitExpression(delphiParser.ExpressionContext ctx) {
        String left = visit(ctx.simpleExpression());

        if (ctx.relationaloperator() != null) {
            String right = visit(ctx.expression());
            String operator = ctx.relationaloperator().getText().toUpperCase();

            String leftType = getType(left);
            String rightType = getType(right);
            String result;

            if (leftType.equals("float") || rightType.equals("float")) {
                left = castTo(left, leftType, "float");
                right = castTo(right, rightType, "float");
                result = newTemp();
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
                left = castTo(left, leftType, "i1");
                right = castTo(right, rightType, "i1");
                result = newTemp();
                switch (operator) {
                    case "=": emit("  " + result + " = icmp eq i1 " + left + ", " + right); break;
                    case "<>": emit("  " + result + " = icmp ne i1 " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else {
                left = castTo(left, leftType, "i32");
                right = castTo(right, rightType, "i32");
                result = newTemp();
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

            String leftType = getType(left);
            String rightType = getType(right);
            String result;

            String resultType = (leftType.equals("float") || rightType.equals("float")) ? "float" : "i32";

            if (leftType.equals("float") || rightType.equals("float")) {
                left = castTo(left, leftType, "float");
                right = castTo(right, rightType, "float");
                result = newTemp();
                switch (operator) {
                    case "+": emit("  " + result + " = fadd float " + left + ", " + right); break;
                    case "-": emit("  " + result + " = fsub float " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else if (operator.equals("OR")) {
                left = castTo(left, leftType, "i1");
                right = castTo(right, rightType, "i1");
                result = newTemp();
                emit("  " + result + " = or i1 " + left + ", " + right);
                resultType = "i1";
            }
            else {
                left = castTo(left, leftType, "i32");
                right = castTo(right, rightType, "i32");
                result = newTemp();
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

            String leftType = getType(left);
            String rightType = getType(right);
            String result;

            String resultType = (leftType.equals("float") || rightType.equals("float")) ? "float" : "i32";

            if (leftType.equals("float") || rightType.equals("float")) {
                if (operator.equals("DIV") || operator.equals("MOD")) {
                    left = castTo(left, leftType, "i32");
                    right = castTo(right, rightType, "i32");
                    result = newTemp();
                    if (operator.equals("DIV")) {
                        emit("  " + result + " = sdiv i32 " + left + ", " + right);
                    } else {
                        emit("  " + result + " = srem i32 " + left + ", " + right);
                    }
                    tempTypes.put(result, "i32");
                    return result;
                }
                left = castTo(left, leftType, "float");
                right = castTo(right, rightType, "float");
                result = newTemp();
                switch (operator) {
                    case "*": emit("  " + result + " = fmul float " + left + ", " + right); break;
                    case "/": emit("  " + result + " = fdiv float " + left + ", " + right); break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else if (leftType.equals("i1") || rightType.equals("i1")) {
                left = castTo(left, leftType, "i1");
                right = castTo(right, rightType, "i1");
                result = newTemp();
                switch (operator) {
                    case "AND":
                        emit("  " + result + " = and i1 " + left + ", " + right);
                        resultType = "i1";
                        break;
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            else {
                left = castTo(left, leftType, "i32");
                right = castTo(right, rightType, "i32");
                result = newTemp();
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
        if (ctx.objectInstantiation() != null) {
            return visit(ctx.objectInstantiation());
        }

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
