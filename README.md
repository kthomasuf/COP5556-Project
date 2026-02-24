# COP5556 - Project #1

### Team Members
Shriyans Arkal & Kenneth Thomas

## What is working?
Our interpreter implementation and grammar additions successfully add functionality for classes, objects, constructors, destructors, and encapsulation to the predefined language. Class inheritance and related functionality was also implemented. In total we created 12 test files to showcase our implementation and demonstrate the parts of the language we added.

## Explanation of Implementation
In order to add the previously listed functionality to the predefined language, several additions were added to the language grammar. Specifically, classes were added as a possible unpackedStructuredType with their accompanying grammar definitions, as well objects were able to be instantiated by adding grammar definitions to the predefined factor definitions. Apart from changes to the grammar we implemented a java interpreter that is able to navigate the abstract syntax tree and provide object oriented programming support that allows classes, objects, etc. to be defined and tested.

## Commands to run our implementation:

### Generate Lexer and Parser

```bash
java -jar antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o my/delphi delphi.g4
```

### Compile Interpreter Code

```bash
javac -cp .:antlr-4.13.2-complete.jar Main.java Value.java Instance.java Environment.java ClassSymbol.java my/delphi/*.java
```

### Run The Interpreter

Modify test1.pas to execute different test files (ex. test2.pas)

```bash
java -cp .:antlr-4.13.2-complete.jar Main test1.pas
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
