# COP5556 - Project #2

## Team Members
Shriyans Arkal (UFID: 52069214)  
Kenneth Thomas (UFID: 47801757)  

## What is working?
For the first project milestone, our interpreter implementation and grammar additions successfully add functionality for classes, objects, constructors, destructors, and encapsulation to the predefined language. Class inheritance and related functionality was also implemented. 

For the second project milestone, we were able to succesfully implement while-do and for-do loops, break and continue keywords, user defined procedures and functions, as well update our language to use static scoping. 

In total we created 21 test files to showcase our implementation and demonstrate the parts of the language we added.

## Explanation of Implementation
In order to add the previously listed functionality to the predefined language, several additions were added to the language grammar. Specifically, classes were added as a possible unpackedStructuredType with their accompanying grammar definitions, as well objects were able to be instantiated by adding grammar definitions to the predefined factor definitions. Apart from changes to the grammar we implemented a java interpreter that is able to navigate the abstract syntax tree and provide object oriented programming support that allows classes, objects, etc. to be defined and tested.

Further updates to the predefined langauge allowed us to implement loops, keywords, and allow users to define their own procedures and functions. Moreover, changes to our interpreter allowed us to add static scoping to the language.

## Commands To Run Our Implementation

### Generate Lexer and Parser

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

### Compile Interpreter Code

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar src/Main.java src/Value.java src/Instance.java src/Environment.java src/ClassSymbol.java src/BreakException.java src/ContinueException.java src/ProcedureSymbol.java src/FunctionSymbol.java src/my/delphi/*.java
```

### Run The Interpreter

Modify test1.pas to execute different test files (ex. test2.pas)

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/test1.pas
```

## Test File Explainations

test1.pas -> Tests Object Oriented Programming Functionality  
test2.pas -> Tests Object Oriented Programming Functionality  
test3.pas -> Tests Arithmetic Logic  
test4.pas -> Tests Comparison Logic  
test5.pas -> Tests Destructor  
test6.pas -> Tests Encapsulation  
test7.pas -> Tests Encapsulation  
test8.pas -> Tests User Input Functionality  
test9.pas -> Tests Inheritance  
test10.pas -> Tests Inheritance w/ Encapsulation  
test11.pas -> Tests All Functionality  
test12.pas -> Tests Object Oriented Programming w/ User Input  
test13.pas -> Tests If-Else statements  
test14.pas -> Tests While Loop  
test15.pas -> Tests For Loop  
test16.pas -> Tests Break Keyword in For Loop  
test17.pas -> Tests Continue Keyword in For Loop  
test18.pas -> Tests Break Keyword in While Loop  
test19.pas -> Tests Continue Keyword in While Loop  
test20.pas -> Tests Static Scoping for Procedures  
test21.pas -> Tests Static Scoping for Functions  