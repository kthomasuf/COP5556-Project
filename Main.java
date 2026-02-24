import org.antlr.v4.runtime.*;
import my.delphi.*;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    
    // extends the base visitor 
    public static class DelphiInterpreter extends delphiBaseVisitor<Value> {
        
        // global scope 
        Environment globals = new Environment();
        // current scope 
        Environment currentEnv = globals;

        // entry point to visit main block
        @Override
        public Value visitProgram(delphiParser.ProgramContext ctx) {
            return visit(ctx.block());
        }

        // visit definitions first and then other statements
        @Override
        public Value visitBlock(delphiParser.BlockContext ctx) {
            for (delphiParser.TypeDefinitionPartContext t : ctx.typeDefinitionPart()) visit(t);
            for (delphiParser.VariableDeclarationPartContext v : ctx.variableDeclarationPart()) visit(v);
            for (delphiParser.ProcedureAndFunctionDeclarationPartContext p : ctx.procedureAndFunctionDeclarationPart()) visit(p);
            return visit(ctx.compoundStatement());
        }

        // definitions

        // registers a new class in the environment
        @Override
        public Value visitTypeDefinition(delphiParser.TypeDefinitionContext ctx) {
            String name = ctx.identifier().getText();
            // confirm it is a class
            if (ctx.type_().structuredType() != null &&
                ctx.type_().structuredType().unpackedStructuredType() != null &&
                ctx.type_().structuredType().unpackedStructuredType().classType() != null) {
                
                ClassSymbol newClass = new ClassSymbol(name);
                delphiParser.ClassTypeContext classCtx = ctx.type_().structuredType().unpackedStructuredType().classType();

                if (classCtx.classParent() != null) {
                    String parentName = classCtx.classParent().identifier().getText();
                    ClassSymbol parent = currentEnv.getClass(parentName);
                    
                    if (parent != null) {
                        newClass.parent = parent;
                        System.out.println("[Interpreter] Class '" + name + "' inherits from '" + parentName + "'");
                    }
                }

                if (classCtx.classBody() != null) {
                    for (delphiParser.ClassSectionContext section : classCtx.classBody().classSection()) {
                        
                        boolean isPrivate = false;
                        if (section.visibilitySpecifier() != null && section.visibilitySpecifier().PRIVATE() != null) {
                            isPrivate = true;
                        }
                        
                        if (section.classMemberList() != null) {
                            for (delphiParser.ClassMemberContext member : section.classMemberList().classMember()) {
                                
                                if (member.fieldDeclaration() != null) {
                                    for (delphiParser.IdentifierContext id : member.fieldDeclaration().identifierList().identifier()) {
                                        if (isPrivate) newClass.privateMembers.add(id.getText().toLowerCase());
                                    }
                                }
                                
                                if (member.procedureHeader() != null) {
                                    String procName = member.procedureHeader().identifier().getText();
                                    if (isPrivate) newClass.privateMembers.add(procName.toLowerCase());
                                }

                                if (member.functionHeader() != null) {
                                    String funcName = member.functionHeader().identifier().getText();
                                    if (isPrivate) newClass.privateMembers.add(funcName.toLowerCase());
                                }
                            }
                        }
                    }
                }

                currentEnv.defineClass(name, newClass);
                System.out.println("[Interpreter] Defined Class Blueprint: " + name);
            }
            return Value.VOID;
        }

        // declares global variables to have value 0 by default
        @Override
        public Value visitVariableDeclaration(delphiParser.VariableDeclarationContext ctx) {
            for (delphiParser.IdentifierContext id : ctx.identifierList().identifier()) {
                String varName = id.getText();
                currentEnv.define(varName, new Value(0)); 
                System.out.println("[Interpreter] Declared Global Var: " + varName);
            }
            return Value.VOID;
        }

        // links class methods to class symbol or just normal global procedure not tied toa class
        @Override
        public Value visitProcedureDeclaration(delphiParser.ProcedureDeclarationContext ctx) {
            String fullName = ctx.scopedIdentifier().getText();
            if (fullName.contains(".")) {
                // method for a class with scoped notation (className.methodName)
                String[] parts = fullName.split("\\.");
                String className = parts[0];
                String methodName = parts[1];
                ClassSymbol clazz = currentEnv.getClass(className);
                if (clazz != null) {
                    clazz.procedures.put(methodName.toLowerCase(), ctx);
                    System.out.println("[Interpreter] Linked Method '" + methodName + "' to Class '" + className + "'");
                }
            } else {
                // normal global procedure
                currentEnv.defineProcedure(fullName, ctx);
                System.out.println("[Interpreter] Defined Global Procedure: " + fullName);
            }
            return Value.VOID;
        }

        @Override
        public Value visitFunctionDeclaration(delphiParser.FunctionDeclarationContext ctx) {
            String fullName = ctx.scopedIdentifier().getText();
            if (fullName.contains(".")) {
                String[] parts = fullName.split("\\.");
                String className = parts[0];
                String funcName = parts[1];
                ClassSymbol clazz = currentEnv.getClass(className);
                if (clazz != null) {
                    clazz.functions.put(funcName.toLowerCase(), ctx);
                    System.out.println("[Interpreter] Linked Function '" + funcName + "' to Class '" + className + "'");
                }
            } else {
                currentEnv.defineFunction(fullName, ctx);
                System.out.println("[Interpreter] Defined Global Function: " + fullName);
            }
            return Value.VOID;
        }

        // links constructor to class symbol
        @Override
        public Value visitConstructorDeclaration(delphiParser.ConstructorDeclarationContext ctx) {
            String fullName = ctx.scopedIdentifier().getText();
            if (fullName.contains(".")) {
                String[] parts = fullName.split("\\.");
                String className = parts[0];
                ClassSymbol clazz = currentEnv.getClass(className);
                if (clazz != null) {
                    clazz.constructorImpl = ctx;
                    System.out.println("[Interpreter] Linked Constructor to Class '" + className + "'");
                }
            }
            return Value.VOID;
        }

        // destructor
        @Override
        public Value visitDestructorDeclaration(delphiParser.DestructorDeclarationContext ctx) {
            String fullName = ctx.scopedIdentifier().getText();
            if (fullName.contains(".")) {
                String[] parts = fullName.split("\\.");
                String className = parts[0];
                ClassSymbol clazz = currentEnv.getClass(className);
                if (clazz != null) {
                    clazz.destructorImpl = ctx;
                    System.out.println("[Interpreter] Linked Destructor to Class '" + className + "'");
                }
            }
            return Value.VOID;
        }

        // inheritance constructor chain helper
        private void runConstructorChain(ClassSymbol targetBlueprint, ClassSymbol currentBlueprint, Value newObj, delphiParser.ParameterListContext passedArgs) {
            if (currentBlueprint.parent != null) {
                runConstructorChain(targetBlueprint, currentBlueprint.parent, newObj, passedArgs);
            }

            if (currentBlueprint.constructorImpl != null) {
                System.out.println("[Runtime] Executing Constructor for " + currentBlueprint.name);
                Environment previous = currentEnv;
                currentEnv = new Environment(previous);
                // inject 'self' into the constructor scope
                currentEnv.define("self", newObj); 
                
                if (currentBlueprint == targetBlueprint) {
                    mapParameters(currentBlueprint.constructorImpl.formalParameterList(), passedArgs);
                } else {
                    mapParameters(currentBlueprint.constructorImpl.formalParameterList(), null);
                }

                visit(currentBlueprint.constructorImpl.block());
                currentEnv = previous;
            }
        }

        // execution

        // creates new object instance and turns constructor
        @Override
        public Value visitObjectInstantiation(delphiParser.ObjectInstantiationContext ctx) {
            String leftName = ctx.identifier(0).getText();
            String rightName = ctx.identifier(1).getText();

            // check class instatiation first 
            ClassSymbol blueprint = currentEnv.getClass(leftName);
            if (blueprint != null) {
                Instance newObj = new Instance(blueprint);
                System.out.println("[Runtime] Allocated memory for: " + leftName);
                
                runConstructorChain(blueprint, blueprint, new Value(newObj), ctx.parameterList());
                return new Value(newObj);
            }

            // method function if not class 
            if (currentEnv.isDefined(leftName)) {
                Value objVal = currentEnv.get(leftName);
                if (objVal.isInstance()) {
                    checkAccess(objVal.asInstance(), rightName);

                    delphiParser.FunctionDeclarationContext methodFunc = findFunction(objVal.asInstance().type, rightName);
                    if (methodFunc != null) {
                        Environment previous = currentEnv;
                        currentEnv = new Environment(previous);
                        currentEnv.define("self", objVal);
                        currentEnv.define(rightName, new Value(0));
                        
                        mapParameters(methodFunc.formalParameterList(), ctx.parameterList());
                        visit(methodFunc.block());
                        
                        Value res = currentEnv.get(rightName);
                        currentEnv = previous;
                        return res;
                    } else {
                        throw new RuntimeException("Method '" + rightName + "' not found in object '" + leftName + "'");
                    }
                }
            }

            throw new RuntimeException("Unknown class or object: " + leftName);
        }

        // locate method code helper function
        private delphiParser.ProcedureDeclarationContext findMethod(ClassSymbol clazz, String methodName) {
            // check base (child) class first
            if (clazz.procedures.containsKey(methodName)) {
                return clazz.procedures.get(methodName);
            }

            // check parent class (recursively for extended parental chains)
            if (clazz.parent != null) {
                return findMethod(clazz.parent, methodName);
            }

            return null;
        }

        private delphiParser.FunctionDeclarationContext findFunction(ClassSymbol clazz, String funcName) {
            if (clazz.functions.containsKey(funcName.toLowerCase())) {
                return clazz.functions.get(funcName.toLowerCase());
            }
            if (clazz.parent != null) {
                return findFunction(clazz.parent, funcName);
            }
            return null;
        }

        // procedure calls like writeln, methods or normal procedures
        @Override
        public Value visitProcedureStatement(delphiParser.ProcedureStatementContext ctx) {
            String callName = ctx.variable().getText();
            
            if (callName.equalsIgnoreCase("WriteLn")) {
                 if (ctx.parameterList() != null) {
                     Value val = visit(ctx.parameterList().actualParameter(0).expression());
                     System.out.println(">> OUTPUT: " + val.toString());
                 }
                 return Value.VOID;
            }

            if (callName.equalsIgnoreCase("ReadLn")) {
                 if (ctx.parameterList() != null) {
                    String varName = ctx.parameterList().actualParameter(0).expression().getText();
                    Scanner scanner = new Scanner(System.in);
                    System.out.println(">> INPUT: ");
                    // Only handles integers for now
                    int input = scanner.nextInt();
                    currentEnv.assign(varName, new Value(input));
                 }
                 return Value.VOID;
            }

            // method call 
            if (callName.contains(".")) {
                String[] parts = callName.split("\\.");
                String instanceName = parts[0];
                String methodName = parts[1];

                Value objVal = currentEnv.get(instanceName);
                if (objVal.asInstance() == null) throw new RuntimeException(instanceName + " is not an object!");

                Instance instance = objVal.asInstance();

                checkAccess(instance, methodName);

                // Check for destructor method -- hardcoding not ideal but works
                if (methodName.equalsIgnoreCase("Destroy")) {
                    if (instance.type.destructorImpl != null) {
                        System.out.println("[Runtime] Executing Destructor");
                        Environment previous = currentEnv;
                        currentEnv = new Environment(previous);
                        currentEnv.define("self", objVal);
                        visit(instance.type.destructorImpl.block());
                        currentEnv = previous;
                    }
                    return Value.VOID;
                }

                delphiParser.ProcedureDeclarationContext methodCode = findMethod(instance.type, methodName.toLowerCase());

                if (methodCode != null) {
                    Environment previous = currentEnv;
                    currentEnv = new Environment(previous); 
                    // set 'self' 
                    currentEnv.define("self", objVal); 

                    mapParameters(methodCode.formalParameterList(), ctx.parameterList());

                    visit(methodCode.block()); 
                    currentEnv = previous;
                } else {
                    throw new RuntimeException("Method " + methodName + " not found");
                }
            } else {
                // calling private method from within the class 
                if (currentEnv.isDefined("self")) {
                    Value selfVal = currentEnv.get("self");
                    if (selfVal.isInstance()) {
                        delphiParser.ProcedureDeclarationContext methodCode = findMethod(selfVal.asInstance().type, callName.toLowerCase());
                        if (methodCode != null) {
                            Environment previous = currentEnv;
                            currentEnv = new Environment(previous);
                            currentEnv.define("self", selfVal);
                            mapParameters(methodCode.formalParameterList(), ctx.parameterList());
                            visit(methodCode.block());
                            currentEnv = previous;
                            return Value.VOID;
                        }
                    }
                }

                // normal global procedure call
                delphiParser.ProcedureDeclarationContext globalProc = currentEnv.getProcedure(callName);
                if (globalProc != null) {
                    Environment previous = currentEnv;
                    currentEnv = new Environment(previous);
                    
                    mapParameters(globalProc.formalParameterList(), ctx.parameterList());

                    visit(globalProc.block());
                    currentEnv = previous;
                } else {
                    // stop silently failing lol
                    throw new RuntimeException("Procedure '" + callName + "' not found");
                }
            }
            return Value.VOID;
        }

        @Override
        public Value visitFunctionDesignator(delphiParser.FunctionDesignatorContext ctx) {
            String funcName = ctx.identifier().getText();
            delphiParser.FunctionDeclarationContext globalFunc = currentEnv.getFunction(funcName);
            
            if (globalFunc != null) {
                Environment previous = currentEnv;
                currentEnv = new Environment(previous);
                
                currentEnv.define(funcName, new Value(0));
                
                mapParameters(globalFunc.formalParameterList(), ctx.parameterList());
                visit(globalFunc.block());
                
                Value result = currentEnv.get(funcName);
                currentEnv = previous;
                return result;
            }
            throw new RuntimeException("Function " + funcName + " not found");
        }

        // variable assignment, explicit and implicit 
        @Override
        public Value visitAssignmentStatement(delphiParser.AssignmentStatementContext ctx) {
            String varName = ctx.variable().getText();
            Value val = visit(ctx.expression());

            if (varName.contains(".")) {
                // explicit field assignment 
                String[] parts = varName.split("\\.");
                String instanceName = parts[0];
                String fieldName = parts[1];
                
                if (!instanceName.equalsIgnoreCase("self")) {
                    Value objVal = currentEnv.get(instanceName);
                    if (objVal.asInstance() != null) {
                        checkAccess(objVal.asInstance(), fieldName);
                    }
                }

                if (instanceName.equalsIgnoreCase("self")) {
                    currentEnv.get("self").asInstance().put(fieldName, val);
                } else {
                    currentEnv.get(instanceName).asInstance().put(fieldName, val);
                }
            } else {
                // implicit lookup 
                if (currentEnv.isDefined(varName)) {
                    currentEnv.assign(varName, val);
                } else if (currentEnv.isDefined("self")) {
                    // implicit field assignment 
                    currentEnv.get("self").asInstance().put(varName, val);
                } else {
                    throw new RuntimeException("Undefined variable '" + varName + "'");
                }
            }
            return Value.VOID;
        }
        
        // visit statement lists
        @Override
        public Value visitCompoundStatement(delphiParser.CompoundStatementContext ctx) {
            visit(ctx.statements());
            return Value.VOID;
        }

        // relational operator support
        @Override
        public Value visitExpression(delphiParser.ExpressionContext ctx) {
            Value left = visit(ctx.simpleExpression());

            if (ctx.relationaloperator() != null) {
                Value right = visit(ctx.expression());
                String operator = ctx.relationaloperator().getText().toUpperCase();

                if (left.isString() || right.isString()) {
                    int cmp = left.asString().compareTo(right.asString());
                    switch (operator) {
                        case "=": return new Value(cmp == 0);
                        case "<>": return new Value(cmp != 0);
                        default: throw new RuntimeException("Operator " + operator + " unsupported for Strings");
                    }
                }

                if (left.isBoolean() || right.isBoolean()) {
                    boolean l = left.asBoolean(), r = right.asBoolean();
                    switch (operator) {
                        case "=": return new Value(l == r);
                        case "<>": return new Value(l != r);
                        default: throw new RuntimeException("Unsupported operator for Boolean");
                    }
                }

                double l = left.asDouble();
                double r = right.asDouble();
                switch (operator) {
                    case "=": return new Value(l == r);
                    case "<>": return new Value(l != r);
                    case "<": return new Value(l < r);
                    case "<=": return new Value(l <= r);
                    case ">": return new Value(l > r);
                    case ">=": return new Value(l >= r);
                    default: throw new RuntimeException("Unknown operator: " + operator);
                }
            }

            return left;
        }
        
        // basic math support, need to add more operators and precedence handling 
        @Override
        public Value visitSimpleExpression(delphiParser.SimpleExpressionContext ctx) {
            Value left = visit(ctx.term());

            if (ctx.additiveoperator() != null) {
                Value right = visit(ctx.simpleExpression());
                String operator = ctx.additiveoperator().getText().toUpperCase();

                switch (operator) {
                    case "+":
                        if (left.isString() || right.isString()) return new Value(left.asString() + right.asString());
                        if (left.isDouble() || right.isDouble()) return new Value(left.asDouble() + right.asDouble());
                        return new Value(left.asInt() + right.asInt());
                    case "-":
                        if (left.isDouble() || right.isDouble()) return new Value(left.asDouble() - right.asDouble());
                        return new Value(left.asInt() - right.asInt());
                    case "OR":
                        if (left.isBoolean() && right.isBoolean()) {
                            return new Value(left.asBoolean() || right.asBoolean());
                        } else {
                            return new Value(left.asInt() | right.asInt()); // Bitwise OR
                        }
                    default:
                        throw new RuntimeException("Unknown operator: " + operator);
                }
            }
            return left;
        }

        // multiplicative math support
        @Override
        public Value visitTerm(delphiParser.TermContext ctx) {
            Value left = visit(ctx.signedFactor());

            if (ctx.multiplicativeoperator() != null) {
                Value right = visit(ctx.term());
                String operator = ctx.multiplicativeoperator().getText().toUpperCase();

                switch (operator) {
                    case "*":
                        if (left.isDouble() || right.isDouble()) return new Value(left.asDouble() * right.asDouble());
                        return new Value(left.asInt() * right.asInt());
                    case "/":
                        return new Value(left.asDouble() / right.asDouble());
                    case "DIV":
                        return new Value(left.asInt() / right.asInt());
                    case "MOD":
                        return new Value(left.asInt() % right.asInt());
                    case "AND":
                        if (left.isBoolean() && right.isBoolean()) {
                            return new Value(left.asBoolean() && right.asBoolean());
                        } else {
                            return new Value(left.asInt() & right.asInt()); // bitwise and
                        }
                    default:
                        throw new RuntimeException("Unknown operator: " + operator);
                }
            }

            return left;
        }

        // check for sign
        @Override
        public Value visitSignedFactor(delphiParser.SignedFactorContext ctx) {
            Value val = visit(ctx.factor());

            if (ctx.MINUS() != null) {
                if (val.isDouble()) return new Value(-val.asDouble());
                return new Value(-val.asInt());
            }

            return val;
        }

        // reading values
        @Override
        public Value visitFactor(delphiParser.FactorContext ctx) {
            if (ctx.bool_() != null) return new Value(ctx.bool_().TRUE() != null); 
            
            if (ctx.NOT() != null) {
                Value val = visit(ctx.factor());
                if (val.isBoolean()) return new Value(!val.asBoolean());
                return new Value(~val.asInt()); //bitwise not for ints
            }
            
            if (ctx.functionDesignator() != null) return visit(ctx.functionDesignator()); 

            if (ctx.LPAREN() != null) return visit(ctx.expression());

            if (ctx.unsignedConstant() != null) {
                String text = ctx.unsignedConstant().getText();
                if (text.startsWith("'")) return new Value(text.replace("'", "")); 
                if (text.contains(".") || text.toLowerCase().contains("e")) {
                    return new Value(Double.parseDouble(text)); 
                }
                return new Value(Integer.parseInt(text));        
            }

            if (ctx.variable() != null) {
                String name = ctx.variable().getText();

                if (!name.contains(".")) {
                    delphiParser.FunctionDeclarationContext func = currentEnv.getFunction(name);
                    if (func != null && !currentEnv.isDefinedLocal(name)) {
                        Environment previous = currentEnv;
                        currentEnv = new Environment(previous);
                        currentEnv.define(name, new Value(0));
                        visit(func.block());
                        Value res = currentEnv.get(name);
                        currentEnv = previous;
                        return res;
                    }
                } else {
                    String[] parts = name.split("\\.");
                    if (currentEnv.isDefined(parts[0])) {
                        Value objVal = currentEnv.get(parts[0]);
                        if (objVal.isInstance()) {
                            delphiParser.FunctionDeclarationContext methodFunc = findFunction(objVal.asInstance().type, parts[1]);
                            if (methodFunc != null && !currentEnv.isDefinedLocal(parts[1])) {
                                Environment previous = currentEnv;
                                currentEnv = new Environment(previous);
                                currentEnv.define("self", objVal);
                                currentEnv.define(parts[1], new Value(0));
                                visit(methodFunc.block());
                                Value res = currentEnv.get(parts[1]);
                                currentEnv = previous;
                                return res;
                            }
                        }
                    }
                }

                if (name.contains(".")) {
                    String[] parts = name.split("\\.");
                    checkAccess(currentEnv.get(parts[0]).asInstance(), parts[1]);
                    return currentEnv.get(parts[0]).asInstance().get(parts[1]);
                }
                
                if (currentEnv.isDefined(name)) {
                    return currentEnv.get(name);
                } else if (currentEnv.isDefined("self")) {
                    Instance selfInstance = currentEnv.get("self").asInstance();
                    checkAccess(selfInstance, name);
                    return currentEnv.get("self").asInstance().get(name);
                } else {
                    throw new RuntimeException("Undefined variable '" + name + "'");
                }
            }
            if (ctx.objectInstantiation() != null) {
                return visit(ctx.objectInstantiation());
            }
            return Value.NULL;
        }

        private void mapParameters(delphiParser.FormalParameterListContext definedParams, 
                                   delphiParser.ParameterListContext passedArgs) {
            if (definedParams == null) return;
            
            boolean fillDefaults = (passedArgs == null);

            int argIndex = 0;
            for (delphiParser.FormalParameterSectionContext section : definedParams.formalParameterSection()) {
                
                if (section.parameterGroup() != null) {
                    for (delphiParser.IdentifierContext id : section.parameterGroup().identifierList().identifier()) {
                        String paramName = id.getText();
                        if (!fillDefaults && argIndex >= passedArgs.actualParameter().size()) {
                            throw new RuntimeException("Too few arguments passed.");
                        }
                        
                        Value argValue = fillDefaults ? new Value(0) : visit(passedArgs.actualParameter(argIndex).expression());
                        currentEnv.define(paramName, argValue);
                        argIndex++;
                    }
                }
            }
        }

        private void checkAccess(Instance targetInstance, String memberName) {
            if (targetInstance.type.isPrivate(memberName)) {
                if (!currentEnv.isDefined("self")) {
                    throw new RuntimeException("Access Denied: '" + memberName + "' is PRIVATE.");
                }
                
                Instance currentSelf = currentEnv.get("self").asInstance();
                if (!currentSelf.type.name.equals(targetInstance.type.name)) {
                     throw new RuntimeException("Access Denied: Cannot access private member '" + memberName + "' of " + targetInstance.type.name);
                }
            }
        }

    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp .:antlr-4.13.2-complete.jar Main <file.pas>");
            return;
        }
        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            delphiLexer lexer = new delphiLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            delphiParser parser = new delphiParser(tokens);
            DelphiInterpreter interpreter = new DelphiInterpreter();
            interpreter.visit(parser.program());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}