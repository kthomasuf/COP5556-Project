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
import java.util.LinkedHashMap;

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
        Map<String, String> methodVisibility = new HashMap<>();
        boolean layoutComplete = false;
    }

    private static class LValue {
        String pointer;
        String type;
    }

    private static class ProcedureInfo {
        String name;
        String ownerClass;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
    }

    private static class FunctionInfo {
        String name;
        String returnType;
        String ownerClass;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
    }

    String currentFunctionName = null;
    String currentFunctionReturnType = null;

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

    private Map<String, ProcedureInfo> procedures = new HashMap<>();
    private Map<String, FunctionInfo> functions = new HashMap<>();

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
        // class metadata has to be available before vars are lowered
        if (isGlobalScope) {
            // class metadata only needs to be processed once at global level
            for (delphiParser.TypeDefinitionPartContext t : ctx.typeDefinitionPart()) visit(t);
            completeClassLayouts();
            emitClassTypes();
        }
        for (delphiParser.VariableDeclarationPartContext v : ctx.variableDeclarationPart()) visit(v);
        for (delphiParser.ProcedureAndFunctionDeclarationPartContext p : ctx.procedureAndFunctionDeclarationPart()) visit(p);
        // emit("define i32 @main() {");
        // emit("entry:");
        // pushScope();
        // visit(ctx.compoundStatement());
        // popScope();
        // if (!blockTerminated) emit("  ret i32 0");
        // emit("}");

        if (isGlobalScope) {
            emit("define i32 @main() {");
            emit("entry:");
            pushScope();
            visit(ctx.compoundStatement());
            popScope();
            if (!blockTerminated) emit("  ret i32 0");
            emit("}");
        }
        else {
            visit(ctx.compoundStatement());
        }
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
                    if (member.fieldDeclaration() != null) {
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
                    else {
                        String methodText = member.getText().toLowerCase();
                        methodText = methodText.replace("procedure", "")
                                            .replace("function", "")
                                            .replace("constructor", "")
                                            .replace("destructor", "")
                                            .split("\\(")[0]
                                            .split(":")[0]
                                            .trim();
                        info.methodVisibility.put(methodText, visibility);
                        System.out.println("[LLVM] Method visibility: " + methodText + " = " + visibility);
                    }
                }
            }
        }

        classes.put(className.toLowerCase(), info);
        return null;
    }

    private void checkMethodVisibility(String className, String methodName) {
        ClassInfo classInfo = classes.get(className.toLowerCase());
        if (classInfo == null) return;
        
        String visibility = classInfo.methodVisibility.get(methodName.toLowerCase());
        if (visibility == null) return;
        
        if (visibility.equals("private")) {
            if (currentClass == null || !currentClass.equalsIgnoreCase(className)) {
                throw new RuntimeException("[LLVM] Access denied: private method '" 
                    + methodName + "' on " + className);
            }
        }
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
    public String visitProcedureDeclaration(delphiParser.ProcedureDeclarationContext ctx) {
        String fullName = ctx.scopedIdentifier().getText();

        ProcedureInfo procInfo = new ProcedureInfo();

        // check if global or class procedure
        if (fullName.contains(".")) {
            // method for a class with scoped notation (className.methodName)
            String[] parts = fullName.split("\\.");
            String className = parts[0];
            String methodName = parts[1];

            procInfo.ownerClass = className;
            procInfo.name = methodName;
        } else {
            // normal global procedure
            procInfo.ownerClass = null;
            procInfo.name = fullName;
        }

        // add parameters to procedure information
        if (ctx.formalParameterList() != null) {
            for (delphiParser.FormalParameterSectionContext section : ctx.formalParameterList().formalParameterSection()) {
                String paramType = toLLVMType(section.parameterGroup().typeIdentifier().getText().toLowerCase());
                for (delphiParser.IdentifierContext id : section.parameterGroup().identifierList().identifier()) {
                    procInfo.params.put(id.getText().toLowerCase(), paramType);
                }
            }
        }

        procedures.put(fullName.toLowerCase(), procInfo);

        // build LLVM procedure header
        String llvmProcName = procInfo.ownerClass != null
            ? procInfo.ownerClass + "_" + procInfo.name
            : procInfo.name;

        StringBuilder params = new StringBuilder();
        if (procInfo.ownerClass != null) {
            ClassInfo classInfo = classes.get(procInfo.ownerClass.toLowerCase());
            if (classInfo != null) {
                params.append("%" + classInfo.name + "* %self");
            }
        }
        // add parameters
        boolean first = procInfo.ownerClass == null;
        for (Map.Entry<String, String> param : procInfo.params.entrySet()) {
            String paramName = param.getKey();
            String paramType = param.getValue();
            if (!first) params.append(", ");
            params.append(paramType).append(" %").append(paramName);
            first = false;
        }

        emit("");
        emit("define void @" + llvmProcName + "(" + params + ") {");
        emit("entry:");

        isGlobalScope = false;
        currentClass = procInfo.ownerClass;
        pushScope();

        // if procedure is a part of class add self as a local variable
        if (procInfo.ownerClass != null) {
            ClassInfo classInfo = classes.get(procInfo.ownerClass.toLowerCase());
            if (classInfo != null) {
                String selfType = "%" + classInfo.name + "*";
                emit("  %self.addr = alloca " + selfType);
                emit("  store " + selfType + " %self, " + selfType + "* %self.addr");
                defineVar("self", "%self.addr");
                varTypes.put("self", selfType);
            }
        }

        // add local variables
        for (Map.Entry<String, String> param : procInfo.params.entrySet()) {
            String paramName = param.getKey();
            String paramType = param.getValue();
            emit("  %" + paramName + ".addr = alloca " + paramType);
            emit("  store " + paramType + " %" + paramName + ", " + paramType + "* %" + paramName + ".addr");
            defineVar(paramName, "%" + paramName + ".addr");
            varTypes.put(paramName, paramType);
        }

        visit(ctx.block());

        emit("  ret void");
        emit("}");

        popScope();
        isGlobalScope = true;
        currentClass = null;

        return null;
    }

    // procedure calls like writeln, methods or normal procedures
    @Override
    public String visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
        String callName = ctx.variable().getText().toLowerCase();

        if (callName.equalsIgnoreCase("WriteLn")) {
            if (ctx.parameterList() != null) {
                emitWriteLn(ctx.parameterList());
            }
            return null;
        }

        if (callName.equalsIgnoreCase("ReadLn")) {
            if (ctx.parameterList() != null) {
                emitReadLn(ctx.parameterList());
            }
            return null;
        }

        // class method
        if (callName.contains(".")) {
            String[] parts = callName.split("\\.");
            String objectName = parts[0];
            String methodName = parts[1];

            // lookup object
            String objectLoc = lookupVar(objectName);
            String className = varClassTypes.get(objectName);
            if (className == null) {
                throw new RuntimeException(objectName + " is not an object!");
            }

            String mapKey = className + "." + methodName;
            ProcedureInfo procInfo = procedures.get(mapKey.toLowerCase());
            if (procInfo == null) {
                throw new RuntimeException("[LLVM] Undefined procedure: " + callName);
            }

            ClassInfo classInfo = classes.get(className.toLowerCase());
            if (classInfo == null) throw new RuntimeException("[LLVM] Unknown class: " + className);
            String originalClassName = classInfo.name;

            // add self register
            String selfReg = newTemp();
            String selfType = "%" + originalClassName + "*";
            emit("  " + selfReg + " = load " + selfType + ", " + selfType + "* " + objectLoc);
            tempTypes.put(selfReg, selfType);

            // self already added
            StringBuilder args = new StringBuilder();
            args.append(selfType).append(" ").append(selfReg);
            if (ctx.parameterList() != null) {
                for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                    args.append(", ");
                    String argReg = visit(param.expression());
                    String argType = getType(argReg);
                    args.append(argType).append(" ").append(argReg);
                }
            }

            checkMethodVisibility(className, methodName);
            emit("  call void @" + originalClassName + "_" + procInfo.name + "(" + args + ")");
            return null;
        }

        // get user defined procedure
        ProcedureInfo procInfo = procedures.get(callName);
        if (procInfo == null) {
            if (currentClass != null) {
                String mapKey = (currentClass + "." + callName).toLowerCase();
                ProcedureInfo classProc = procedures.get(mapKey);
                if (classProc != null) {
                    String selfLoc = lookupVar("self");
                    String originalClassName = classes.get(currentClass.toLowerCase()).name;
                    String selfType = "%" + originalClassName + "*";
                    String selfReg = newTemp();
                    emit("  " + selfReg + " = load " + selfType + ", " + selfType + "* " + selfLoc);
                    tempTypes.put(selfReg, selfType);
                    emit("  call void @" + originalClassName + "_" + classProc.name + "(" + selfType + " " + selfReg + ")");
                    return null;
                }
            }
            throw new RuntimeException("[LLVM] Undefined procedure: " + callName);
        }

        // given procedure define passed in arguments
        StringBuilder args = new StringBuilder();
        if (ctx.parameterList() != null) {
            boolean first = true;
            for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                if (!first) args.append(", ");
                String argReg = visit(param.expression());
                String argType = getType(argReg);
                args.append(argType).append(" ").append(argReg);
                first = false;
            }
        }

        emit("  call void @" + procInfo.name + "(" + args + ")");
        return null;
    }

    @Override
    public String visitFunctionDeclaration(delphiParser.FunctionDeclarationContext ctx) {
        String fullName = ctx.scopedIdentifier().getText();

        FunctionInfo funcInfo = new FunctionInfo();

        // check if global or class function
        if (fullName.contains(".")) {
            // method for a class with scoped notation (className.methodName)
            String[] parts = fullName.split("\\.");
            String className = parts[0];
            String methodName = parts[1];

            funcInfo.ownerClass = className;
            funcInfo.name = methodName;
        } else {
            // normal global procedure
            funcInfo.ownerClass = null;
            funcInfo.name = fullName;
        }

        // add parameters to procedure information
        if (ctx.formalParameterList() != null) {
            for (delphiParser.FormalParameterSectionContext section : ctx.formalParameterList().formalParameterSection()) {
                String paramType = toLLVMType(section.parameterGroup().typeIdentifier().getText().toLowerCase());
                for (delphiParser.IdentifierContext id : section.parameterGroup().identifierList().identifier()) {
                    funcInfo.params.put(id.getText().toLowerCase(), paramType);
                }
            }
        }

        String llvmReturnType = toLLVMType(ctx.resultType().getText().toLowerCase());
        funcInfo.returnType = llvmReturnType;

        functions.put(fullName.toLowerCase(), funcInfo);

        // build LLVM function header
        String llvmFuncName = funcInfo.ownerClass != null
            ? funcInfo.ownerClass + "_" + funcInfo.name
            : funcInfo.name;

        StringBuilder params = new StringBuilder();
        if (funcInfo.ownerClass != null) {
            ClassInfo classInfo = classes.get(funcInfo.ownerClass.toLowerCase());
            if (classInfo != null) {
                params.append("%" + classInfo.name + "* %self");
            }
        }
        // add parameters
        boolean first = funcInfo.ownerClass == null;
        for (Map.Entry<String, String> param : funcInfo.params.entrySet()) {
            String paramName = param.getKey();
            String paramType = param.getValue();
            if (!first) params.append(", ");
            params.append(paramType).append(" %").append(paramName);
            first = false;
        }

        emit("");
        emit("define " + llvmReturnType + " @" + llvmFuncName + "(" + params + ") {");
        emit("entry:");

        isGlobalScope = false;
        pushScope();

        // if function is a part of class add self as a local variable
        if (funcInfo.ownerClass != null) {
            ClassInfo classInfo = classes.get(funcInfo.ownerClass.toLowerCase());
            if (classInfo != null) {
                String selfType = "%" + classInfo.name + "*";
                emit("  %self.addr = alloca " + selfType);
                emit("  store " + selfType + " %self, " + selfType + "* %self.addr");
                defineVar("self", "%self.addr");
                varTypes.put("self", selfType);
            }
        }

        currentFunctionName = funcInfo.name.toLowerCase();
        currentFunctionReturnType = llvmReturnType;

        emit("  %result.addr = alloca " + llvmReturnType);
        emit("  store " + llvmReturnType + " " + defaultValue(llvmReturnType) + ", " + llvmReturnType + "* %result.addr");
        defineVar(currentFunctionName, "%result.addr");
        varTypes.put(currentFunctionName, llvmReturnType);

        // add local variables
        for (Map.Entry<String, String> param : funcInfo.params.entrySet()) {
            String paramName = param.getKey();
            String paramType = param.getValue();
            emit("  %" + paramName + ".addr = alloca " + paramType);
            emit("  store " + paramType + " %" + paramName + ", " + paramType + "* %" + paramName + ".addr");
            defineVar(paramName, "%" + paramName + ".addr");
            varTypes.put(paramName, paramType);
        }

        visit(ctx.block());

        // load return value for functions
        String retVal = newTemp();
        emit("  " + retVal + " = load " + llvmReturnType + ", " + llvmReturnType + "* %result.addr");
        emit("  ret " + llvmReturnType + " " + retVal);
        emit("}");

        popScope();
        isGlobalScope = true;

        currentFunctionName = null;
        currentFunctionReturnType = null;
        return null;
    }

    @Override
    public String visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {
        String callName = ctx.identifier().getText().toLowerCase();

        // class method
        if (callName.contains(".")) {
            String[] parts = callName.split("\\.");
            String objectName = parts[0];
            String methodName = parts[1];

            // lookup object
            String objectLoc = lookupVar(objectName);
            String className = varClassTypes.get(objectName);
            if (className == null) {
                throw new RuntimeException(objectName + " is not an object!");
            }

            String mapKey = className + "." + methodName;  
            FunctionInfo funcInfo = functions.get(mapKey);
            if (funcInfo == null) {
                throw new RuntimeException("[LLVM] Undefined function: " + callName);
            }

            // add self register
            String selfReg = newTemp();
            String selfType = "%" + funcInfo.ownerClass + "*";
            emit("  " + selfReg + " = load " + selfType + ", " + selfType + "* " + objectLoc);
            tempTypes.put(selfReg, selfType);

            // self already added
            StringBuilder args = new StringBuilder();
            args.append(selfType).append(" ").append(selfReg);
            if (ctx.parameterList() != null) {
                for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                    args.append(", ");
                    String argReg = visit(param.expression());
                    String argType = getType(argReg);
                    args.append(argType).append(" ").append(argReg);
                }
            }

            String result = newTemp();
            checkMethodVisibility(className, methodName);
            emit("  " + result + " = call " + funcInfo.returnType + " @" + className + "_" + methodName + "(" + args + ")");
            tempTypes.put(result, funcInfo.returnType);
            
            return result;
        }

        // get user defined procedure
        FunctionInfo funcInfo = functions.get(callName);
        if (funcInfo == null) {
            if (currentClass != null) {
                String mapKey = (currentClass + "." + callName).toLowerCase();
                FunctionInfo classFunc = functions.get(mapKey);
                if (classFunc != null) {
                    ClassInfo classInfo = classes.get(currentClass.toLowerCase());
                    if (classInfo != null) {
                        String vis = classInfo.methodVisibility.get(callName.toLowerCase());
                        if (vis != null && vis.equals("private") &&
                            !currentClass.equalsIgnoreCase(classFunc.ownerClass)) {
                            throw new RuntimeException("[LLVM] Access denied: private method '" + callName + "'");
                        }
                    }

                    String selfLoc = lookupVar("self");
                    String originalClassName = classes.get(currentClass.toLowerCase()).name;
                    String selfType = "%" + originalClassName + "*";
                    String selfReg = newTemp();
                    emit("  " + selfReg + " = load " + selfType + ", " + selfType + "* " + selfLoc);
                    tempTypes.put(selfReg, selfType);

                    StringBuilder args = new StringBuilder();
                    args.append(selfType).append(" ").append(selfReg);
                    if (ctx.parameterList() != null) {
                        for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                            args.append(", ");
                            String argReg = visit(param.expression());
                            String argType = getType(argReg);
                            args.append(argType).append(" ").append(argReg);
                        }
                    }

                    String result = newTemp();
                    emit("  " + result + " = call " + classFunc.returnType +
                        " @" + originalClassName + "_" + classFunc.name +
                        "(" + args + ")");
                    tempTypes.put(result, classFunc.returnType);
                    return result;
                }
            }
            throw new RuntimeException("[LLVM] Undefined function: " + callName);
        }

        // given function define passed in arguments
        StringBuilder args = new StringBuilder();
        if (ctx.parameterList() != null) {
            boolean first = true;
            for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                if (!first) args.append(", ");
                String argReg = visit(param.expression());
                String argType = getType(argReg);
                args.append(argType).append(" ").append(argReg);
                first = false;
            }
        }

        // store result in temp register
        String result = newTemp();
        emit("  " + result + " = call " + funcInfo.returnType + " @" + callName + "(" + args + ")");
        tempTypes.put(result, funcInfo.returnType);

        return result;
    }

    @Override
    public String visitConstructorDeclaration(delphiParser.ConstructorDeclarationContext ctx) {
        String fullName = ctx.scopedIdentifier().getText();
        if (fullName.contains(".")) {
            String[] parts = fullName.split("\\.");
            String className = parts[0];
            String constructorName = parts[1];

            ClassInfo classInfo = classes.get(className.toLowerCase());
            if (classInfo == null) throw new RuntimeException("[LLVM] Unknown class: " + className);

            String selfType = "%" + classInfo.name + "*";
            String llvmName = className + "_" + constructorName;

            // collect parameters, needs own map since does use procedure infastructure
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            if (ctx.formalParameterList() != null) {
                for (delphiParser.FormalParameterSectionContext section : ctx.formalParameterList().formalParameterSection()) {
                    String paramType = toLLVMType(section.parameterGroup().typeIdentifier().getText().toLowerCase());
                    for (delphiParser.IdentifierContext id : section.parameterGroup().identifierList().identifier()) {
                        params.put(id.getText().toLowerCase(), paramType);
                    }
                }
            }

            StringBuilder paramStr = new StringBuilder();
            paramStr.append(selfType).append(" %self");
            for (Map.Entry<String, String> param : params.entrySet()) {
                paramStr.append(", ").append(param.getValue()).append(" %").append(param.getKey());
            }

            emit("");
            emit("define void @" + llvmName + "(" + paramStr + ") {");
            emit("entry:");

            isGlobalScope = false;
            pushScope();

            // define self as local variable
            emit("  %self.addr = alloca " + selfType);
            emit("  store " + selfType + " %self, " + selfType + "* %self.addr");
            defineVar("self", "%self.addr");
            varTypes.put("self", selfType);

            // define parameters as local variables
            for (Map.Entry<String, String> param : params.entrySet()) {
                String paramName = param.getKey();
                String paramType = param.getValue();
                emit("  %" + paramName + ".addr = alloca " + paramType);
                emit("  store " + paramType + " %" + paramName + ", " + paramType + "* %" + paramName + ".addr");
                defineVar(paramName, "%" + paramName + ".addr");
                varTypes.put(paramName, paramType);
            }

            // visit constructor body
            visit(ctx.block());

            emit("  ret void");
            emit("}");

            popScope();
            isGlobalScope = true;
        }
        return null;
    }

    @Override
    public String visitDestructorDeclaration(delphiParser.DestructorDeclarationContext ctx) {
        String fullName = ctx.scopedIdentifier().getText();
        if (fullName.contains(".")) {
            String[] parts = fullName.split("\\.");
            String className = parts[0];
            String destructorName = parts[1];
            
            ClassInfo classInfo = classes.get(className.toLowerCase());
            if (classInfo == null) throw new RuntimeException("[LLVM] Unknown class: " + className);

            ProcedureInfo destructorInfo = new ProcedureInfo();
            destructorInfo.name = destructorName;
            destructorInfo.ownerClass = className;
            procedures.put((className + "." + destructorName).toLowerCase(), destructorInfo);

            String selfType = "%" + classInfo.name + "*";
            String llvmName = className + "_" + destructorName;

            emit("");
            emit("define void @" + llvmName + "(" + selfType + " %self) {");
            emit("entry:");

            isGlobalScope = false;
            pushScope();

            // define self as local variable
            emit("  %self.addr = alloca " + selfType);
            emit("  store " + selfType + " %self, " + selfType + "* %self.addr");
            defineVar("self", "%self.addr");
            varTypes.put("self", selfType);

            // visit destructor body
            visit(ctx.block());

            emit("  ret void");
            emit("}");

            popScope();
            isGlobalScope = true;
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
        String methodName = ctx.identifier(1) != null ? ctx.identifier(1).getText() : "Init";

        ClassInfo info = classes.get(className.toLowerCase());

        // not a known class, could be an instance variable calling a method
        if (info == null) {
            String objectName = className.toLowerCase();
            String varClassName = varClassTypes.get(objectName);
            if (varClassName == null) throw new RuntimeException("[LLVM] Unknown class or object: " + className);

            ClassInfo classInfo = classes.get(varClassName.toLowerCase());
            if (classInfo == null) throw new RuntimeException("[LLVM] Unknown class: " + varClassName);

            // check procedures first then functions
            ProcedureInfo procInfo = procedures.get((varClassName + "." + methodName).toLowerCase());
            FunctionInfo funcInfo = functions.get((varClassName + "." + methodName).toLowerCase());

            String objectLoc = lookupVar(objectName);
            String selfReg = newTemp();
            String selfType = "%" + classInfo.name + "*";
            emit("  " + selfReg + " = load " + selfType + ", " + selfType + "* " + objectLoc);
            tempTypes.put(selfReg, selfType);

            StringBuilder args = new StringBuilder();
            args.append(selfType).append(" ").append(selfReg);
            if (ctx.parameterList() != null) {
                for (delphiParser.ActualParameterContext param : ctx.parameterList().actualParameter()) {
                    args.append(", ");
                    String argReg = visit(param.expression());
                    String argType = getType(argReg);
                    args.append(argType).append(" ").append(argReg);
                }
            }
            
            checkMethodVisibility(varClassName, methodName);

            if (funcInfo != null) {
                // function — return value
                String result = newTemp();
                emit("  " + result + " = call " + funcInfo.returnType +
                    " @" + classInfo.name + "_" + methodName + "(" + args + ")");
                tempTypes.put(result, funcInfo.returnType);
                return result;
            } 
            else if (procInfo != null) {
                // procedure — no return value
                emit("  call void @" + classInfo.name + "_" + methodName + "(" + args + ")");
                return null;
            } 
            else {
                throw new RuntimeException("[LLVM] Undefined method: " + varClassName + "." + methodName);
            }
        }

        // known class — normal object instantiation
        String raw = newTemp();
        String object = newTemp();
        emit("  " + raw + " = call i8* @malloc(i64 " + classSize(info) + ")");
        emit("  " + object + " = bitcast i8* " + raw + " to %" + info.name + "*");
        setType(raw, "i8*");
        setType(object, "%" + info.name + "*");
        emit("  call void @" + className + "_" + methodName + "(%" + info.name + "* " + object + ")");
        return object;
    }

    // @Override
    // public String visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
    //     String callName = ctx.variable().getText();
    //     if (callName.equalsIgnoreCase("WriteLn")) {
    //         emitWriteLn(ctx.parameterList());
    //         return null;
    //     }
    //     if (callName.equalsIgnoreCase("ReadLn")) {
    //         emitReadLn(ctx.parameterList());
    //         return null;
    //     }
    //     return super.visitProcedureStatement(ctx);
    // }

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
