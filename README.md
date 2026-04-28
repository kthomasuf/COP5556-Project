# COP5556 - Project #2

## Team Members
Shriyans Arkal (UFID: 52069214)  
Kenneth Thomas (UFID: 47801757)

## What is working?

For the first project milestone, our interpreter implementation and grammar additions successfully add functionality for classes, objects, constructors, destructors, and encapsulation to the predefined language. Class inheritance and related functionality was also implemented.

For the second project milestone, we were able to succesfully implement while-do and for-do loops, break and continue keywords, user defined procedures and functions, as well update our language to use static scoping.

In total we created 21 test files to showcase our implementation and demonstrate the parts of the language we added.

Additional test files were added for automation and edge coverage (tests 22 through 26).

## Explanation of Implementation

In order to add the previously listed functionality to the predefined language, several additions were added to the language grammar. Specifically, classes were added as a possible unpackedStructuredType with their accompanying grammar definitions, as well objects were able to be instantiated by adding grammar definitions to the predefined factor definitions. Apart from changes to the grammar we implemented a java interpreter that is able to navigate the abstract syntax tree and provide object oriented programming support that allows classes, objects, etc. to be defined and tested.

Further updates to the predefined langauge allowed us to implement loops, keywords, and allow users to define their own procedures and functions. Moreover, changes to our interpreter allowed us to add static scoping to the language.

## Commands To Run Our Implementation
To avoid potential complications, please fun the following commands from the project root directory.

### Generate Lexer and Parser

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

### Compile Interpreter Code

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar src/Main.java src/Value.java src/Instance.java src/Environment.java src/ClassSymbol.java src/BreakException.java src/ContinueException.java src/ProcedureSymbol.java src/FunctionSymbol.java src/DelphiInterpreter.java src/LLVMCodeGenerator.java src/my/delphi/*.java
```

### Run The Interpreter

Modify test1.pas to execute different test files (ex. test2.pas)

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/test1.pas
```

### Run LLVM Compile Mode

Generate LLVM IR for a Pascal/Delphi source file:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/compiler/core_arithmetic_io.pas --compile
```

This writes LLVM IR to `output.ll`.

To compile the generated IR to a native executable:

```bash
clang output.ll -o output
./output
```

## Automated Test Runner

A bash-based automated test runner is included.

```bash
bash run_tests.sh
```

The runner compares expected outputs in tests/expected against actual outputs and reports PASS, FAIL, XFAIL, and SKIP.

Interactive ReadLn tests are also automated using simulated input values:

- test8 uses input 42
- test12 uses input 250

Expected-failure tests currently include:

- test6 (private field access violation)
- test7 (private method access violation)
- test24 (BREAK outside loop)
- test25 (CONTINUE outside loop)

## LLVM Compiler Test Runner

A separate automated runner is included for LLVM compile mode:

```bash
bash run_compiler_tests.sh
```

This runner compiles the Java sources, generates LLVM IR for each compiler-focused Pascal test, validates the IR with `clang`, runs the native executable, and reports PASS, FAIL, and XFAIL results.

Current compiler coverage includes:

- primitive values and assignments
- arithmetic, comparison, and boolean expressions
- `WriteLn` and `ReadLn`
- `if/else`, `while`, `for`, `break`, `continue`
- global procedures and functions
- class fields, methods, constructors, destructors
- private field/method access checks

## LLVM Compiler Support Summary

The LLVM backend currently supports a substantial subset of the Project 2 language, including:

- `INTEGER`, `REAL`, `BOOLEAN`
- variable declarations and assignments
- arithmetic/comparison/boolean expressions
- `WriteLn` / `ReadLn`
- `if/else`
- `while`
- `for`
- `break` / `continue`
- global procedures/functions
- classes, objects, constructors, destructors
- private/public enforcement for fields and methods
- inherited field layout

The interpreter remains the broader reference implementation, while the LLVM backend focuses on the chosen compiler subset required by the project.

## WebAssembly Extra Credit

A small WebAssembly demo path is included for the LLVM backend:

- Pascal source: `tests/compiler/wasm_export_demo.pas`
- browser files: `wasm/index.html` and `wasm/runtime.js`
- build helper: `build_wasm_demo.sh`

Build the demo object/module with:

```bash
bash build_wasm_demo.sh
```

Notes:

- The script always generates a WebAssembly object file (`wasm/demo.o`) using `llc`.
- If `wasm-ld` is available, it also links a browser-loadable `wasm/demo.wasm`.
- In the current local environment, `llc` is installed but `wasm-ld` is not, so final `.wasm` linking may require installing the LLVM linker tools.

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
test22.pas -> Tests nested loops with break/continue  
test23.pas -> Tests for-loop boundary conditions  
test24.pas -> Tests invalid BREAK outside loop (expected failure)  
test25.pas -> Tests invalid CONTINUE outside loop (expected failure)  
test26.pas -> Tests while-loop boundary conditions  
