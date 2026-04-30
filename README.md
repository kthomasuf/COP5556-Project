# COP5556 - Final Project

## Team Members
Shriyans Arkal (UFID: 52069214)  
Kenneth Thomas (UFID: 47801757)

## LLVM Video Demonstrations:
General LLVM Compiler Demo: https://youtu.be/AFiSdxOjaMo

WebAssembly Browser Demo: https://youtu.be/TUbR1hKPc1A

## What is working?

For the first project milestone, our interpreter implementation and grammar additions successfully add functionality for classes, objects, constructors, destructors, and encapsulation to the predefined language. Class inheritance and related functionality was also implemented.

For the second project milestone, we were able to succesfully implement while-do and for-do loops, break and continue keywords, user defined procedures and functions, as well update our language to use static scoping. In total we created 21 test files to showcase our implementation and demonstrate the parts of the language we added. Additional test files were added for automation and edge coverage (tests 22 through 26).

For the final project, we were able to implement an LLVM IR compiler that converted our testcases to accurate LLVM intermediate representation code. In terms of features we were able to successfully add classes, procedures, functions, general artihmetic, control flow features related to for loops, while loops, if-else statements, break, etc.

ANTLR is included in `lib/antlr-4.13.2-complete.jar`.

## Prerequisites

Make sure these tools are installed and available on your `PATH`:

- `java`
- `javac`
- `clang`
- `llc`
- `wasm-ld`

## Compiler

### Build The Compiler

After downloading or unzipping the full project, open a terminal in the project root and run:

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar \
    src/Main.java \
    src/Value.java \
    src/Instance.java \
    src/Environment.java \
    src/ClassSymbol.java \
    src/BreakException.java \
    src/ContinueException.java \
    src/ProcedureSymbol.java \
    src/FunctionSymbol.java \
    src/DelphiInterpreter.java \
    src/LLVMCodeGenerator.java \
    src/my/delphi/*.java
```

### Run The Compiler Manually

Generate LLVM IR for a compiler test file:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/compiler/core_arithmetic_io.pas --compile
```

This writes the generated LLVM IR to:

```text
output.ll
```

To validate and run the generated native output:

```bash
clang output.ll -o output
./output
```

### Run The Compiler Test Suite

Use the automated compiler runner:

```bash
bash run_compiler_tests.sh
```

This:

1. compiles the Java sources
2. generates LLVM IR for each compiler-focused Pascal file
3. validates the IR with `clang`
4. runs the resulting native executable
5. reports `PASS`, `FAIL`, and `XFAIL`

### Saved `.ll` Files For Compiler Test Cases

Saved LLVM IR files for the compiler test cases are included in:

```text
tests/compiler/ll/
```

Expected-failure compiler tests include small placeholder `.ll` files explaining that those sources intentionally fail during compilation/code generation.

### Manual Fallback For Compiler Testing

If `run_compiler_tests.sh` does not work in your environment, use the manual flow:

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar \
    src/Main.java \
    src/Value.java \
    src/Instance.java \
    src/Environment.java \
    src/ClassSymbol.java \
    src/BreakException.java \
    src/ContinueException.java \
    src/ProcedureSymbol.java \
    src/FunctionSymbol.java \
    src/DelphiInterpreter.java \
    src/LLVMCodeGenerator.java \
    src/my/delphi/*.java
```

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/compiler/core_arithmetic_io.pas --compile
clang output.ll -o output
./output
```

Replace `tests/compiler/core_arithmetic_io.pas` with any supported compiler test file.

## Interpreter

### Build The Interpreter

The interpreter uses the same generated parser and compiled Java sources as the compiler, so the same build commands apply:

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar \
    src/Main.java \
    src/Value.java \
    src/Instance.java \
    src/Environment.java \
    src/ClassSymbol.java \
    src/BreakException.java \
    src/ContinueException.java \
    src/ProcedureSymbol.java \
    src/FunctionSymbol.java \
    src/DelphiInterpreter.java \
    src/LLVMCodeGenerator.java \
    src/my/delphi/*.java
```

### Run The Interpreter Manually

Run a test file through the interpreter:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/test1.pas
```

### Run The Interpreter Test Suite

Use the automated interpreter runner:

```bash
bash run_tests.sh
```

This compares expected output files against interpreter output and reports `PASS`, `FAIL`, `XFAIL`, and `SKIP`.

### Manual Fallback For Interpreter Testing

If `run_tests.sh` does not work in your environment, use the manual flow:

```bash
java -jar lib/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o src/my/delphi grammar/delphi.g4
```

```bash
javac -cp .:lib/antlr-4.13.2-complete.jar \
    src/Main.java \
    src/Value.java \
    src/Instance.java \
    src/Environment.java \
    src/ClassSymbol.java \
    src/BreakException.java \
    src/ContinueException.java \
    src/ProcedureSymbol.java \
    src/FunctionSymbol.java \
    src/DelphiInterpreter.java \
    src/LLVMCodeGenerator.java \
    src/my/delphi/*.java
```

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/test1.pas
```

Replace `tests/test1.pas` with any interpreter test file you want to run.

## WebAssembly Extra Credit

The WebAssembly demo files are in:

```text
wasm/wasm_export_demo.pas
wasm/index.html
wasm/runtime.js
wasm/README.md
```

The current showcase exports:

- `AddOne`
- `MaxValue`
- `SumToN`
- `CounterDemo`

The full WASM-specific instructions are also documented in:

```text
wasm/README.md
```

### Build The WASM Demo

Use the helper script:

```bash
bash build_wasm_demo.sh
```

Run that command from the downloaded project root directory, not from inside `wasm/`.

The script also saves the LLVM IR used for the WASM demo in:

```text
wasm/output.ll
```

### Manual Fallback

If `build_wasm_demo.sh` does not work, use the manual flow below from the downloaded project root:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main wasm/wasm_export_demo.pas --compile
```

```bash
llc -march=wasm32 -filetype=obj output.ll -o wasm/demo.o
wasm-ld \
    --no-entry \
    --export=AddOne \
    --export=MaxValue \
    --export=SumToN \
    --export=CounterDemo \
    --export=main \
    --allow-undefined \
    wasm/demo.o \
    -o wasm/demo.wasm
```

```bash
python3 -m http.server 8000
```

Start the HTTP server from the downloaded project root directory, not from inside `wasm/`.

That way the browser can load:

- `wasm/index.html`
- `wasm/runtime.js`
- `wasm/demo.wasm`

Then open:

```text
http://127.0.0.1:8000/wasm/
```

## Important Generated Files

- `output.ll` - generated LLVM IR
- `wasm/demo.o` - WebAssembly object file produced by `llc`
- `wasm/demo.wasm` - final linked WebAssembly module used by the browser

