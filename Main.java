import org.antlr.v4.runtime.*;
import my.delphi.*;
import java.io.IOException;

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

        // execution

        // creates new object instance and turns constructor
        @Override
        public Value visitObjectInstantiation(delphiParser.ObjectInstantiationContext ctx) {
            String className = ctx.identifier(0).getText();
            ClassSymbol blueprint = currentEnv.getClass(className);
            if (blueprint == null) throw new RuntimeException("Unknown class: " + className);

            // create the instance 
            Instance newObj = new Instance(blueprint);
            System.out.println("[Runtime] Allocated memory for: " + className);

            // run constructor if it is defined
            if (blueprint.constructorImpl != null) {
                System.out.println("[Runtime] Executing Constructor for " + className);
                Environment previous = currentEnv;
                currentEnv = new Environment(previous);
                // inject 'self' into the constructor scope
                currentEnv.define("self", new Value(newObj)); 
                visit(blueprint.constructorImpl.block());
                currentEnv = previous; 
            }
            return new Value(newObj);
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

            // method call 
            if (callName.contains(".")) {
                String[] parts = callName.split("\\.");
                String instanceName = parts[0];
                String methodName = parts[1];

                Value objVal = currentEnv.get(instanceName);
                if (objVal.asInstance() == null) throw new RuntimeException(instanceName + " is not an object!");

                Instance instance = objVal.asInstance();

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

                delphiParser.ProcedureDeclarationContext methodCode = instance.type.procedures.get(methodName.toLowerCase());
                
                if (methodCode != null) {
                    Environment previous = currentEnv;
                    currentEnv = new Environment(previous); 
                    // set 'self' 
                    currentEnv.define("self", objVal); 
                    visit(methodCode.block()); 
                    currentEnv = previous;
                } else {
                    throw new RuntimeException("Method " + methodName + " not found");
                }
            } else {
                // normal global procdure call
                delphiParser.ProcedureDeclarationContext globalProc = currentEnv.getProcedure(callName);
                if (globalProc != null) {
                    Environment previous = currentEnv;
                    currentEnv = new Environment(previous);
                    visit(globalProc.block());
                    currentEnv = previous;
                }
            }
            return Value.VOID;
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
                String operator = ctx.relationaloperator().getText();

                switch (operator.toUpperCase()) {
                    case "=":
                        return new Value((left.asInt() == right.asInt()) ? 1 : 0);
                    case "<>":
                        return new Value((left.asInt() != right.asInt()) ? 1 : 0);
                    case "<":
                        return new Value((left.asInt() < right.asInt()) ? 1 : 0);
                    case "<=":
                        return new Value((left.asInt() <= right.asInt()) ? 1 : 0);
                    case ">":
                        return new Value((left.asInt() > right.asInt()) ? 1 : 0);
                    case ">=":
                        return new Value((left.asInt() >= right.asInt()) ? 1 : 0);
                    case "IN":
                        // Requires set support
                        throw new RuntimeException("IN operator not yet implemented");
                    default:
                        throw new RuntimeException("Unknown operator: " + operator);
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
                String operator = ctx.additiveoperator().getText();

                switch (operator.toUpperCase()) {
                    case "+":
                        return new Value(left.asInt() + right.asInt());
                    case "-":
                        return new Value(left.asInt() - right.asInt());
                    case "OR":
                        return new Value((left.asInt() != 0 || right.asInt() != 0) ? 1 : 0);
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
                String operator = ctx.multiplicativeoperator().getText();

                switch (operator.toUpperCase()) {
                    case "*":
                        return new Value(left.asInt() * right.asInt());
                    case "/":
                        return new Value(left.asInt() / right.asInt());
                    case "DIV":
                        return new Value(left.asInt() / right.asInt());
                    case "MOD":
                        return new Value(left.asInt() % right.asInt());
                    case "AND":
                        return new Value((left.asInt() != 0 && right.asInt() != 0) ? 1 : 0);
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
                return new Value(-val.asInt());
            }

            return val;
        }

        // reading values
        @Override
        public Value visitFactor(delphiParser.FactorContext ctx) {
            if (ctx.unsignedConstant() != null) {
                String text = ctx.unsignedConstant().getText();
                if (text.startsWith("'")) return new Value(text.replace("'", "")); 
                return new Value(Integer.parseInt(text));        
            }
            if (ctx.variable() != null) {
                String name = ctx.variable().getText();
                if (name.contains(".")) {
                    // explicit field access
                    String[] parts = name.split("\\.");
                    return currentEnv.get(parts[0]).asInstance().get(parts[1]);
                }
                
                // implicit lookup 
                if (currentEnv.isDefined(name)) {
                    return currentEnv.get(name);
                } else if (currentEnv.isDefined("self")) {
                    return currentEnv.get("self").asInstance().get(name);
                } else {
                    throw new RuntimeException("Undefined variable '" + name + "'");
                }
            }
            if (ctx.objectInstantiation() != null) {
                System.out.println("[DEBUG] Recognized as objectInstantiation");
                return visit(ctx.objectInstantiation());
            }
            return Value.NULL;
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