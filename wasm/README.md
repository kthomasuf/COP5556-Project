# WASM Demo

This folder contains the browser-side files for the LLVM WebAssembly extra-credit path.

## Files

- `wasm_export_demo.pas` - Pascal source used for the browser demo
- `index.html` - feature showcase page
- `runtime.js` - loads `demo.wasm`, provides a small `malloc` import, and calls the exported functions
- `output.ll` - LLVM IR for the WASM demo before lowering to WebAssembly
- `demo.o` - WebAssembly object file produced by `llc`
- `demo.wasm` - final browser-loadable WebAssembly module

## Showcase Exports

The current demo exports:

- `AddOne`
- `MaxValue`
- `SumToN`
- `CounterDemo`

These cover:

- arithmetic
- conditionals
- loops
- class allocation and method calls

## Build With The Helper Script

After downloading or unzipping the full project, open a terminal in the project root and run:

```bash
bash build_wasm_demo.sh
```

Do not run this from inside the `wasm/` folder. Run it from the project root.

That script:

1. compiles the Java sources
2. generates LLVM IR from `wasm/wasm_export_demo.pas`
3. uses `llc` to create `wasm/demo.o`
4. uses `wasm-ld` to create `wasm/demo.wasm`

## Manual Fallback If The Script Does Not Work

Run these commands from the downloaded project root.

### 1. Build The Java Compiler

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

### 2. Generate LLVM IR

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main wasm/wasm_export_demo.pas --compile
```

This writes:

```text
output.ll
```

### 3. Compile From `.ll` To `.wasm`

First, lower the LLVM IR into a WebAssembly object file:

```bash
llc -march=wasm32 -filetype=obj output.ll -o wasm/demo.o
```

Then link the object file into a browser-loadable `.wasm` module:

```bash
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

## Run The Browser Demo

Because browsers fetch `.wasm` over HTTP, serve the project directory locally:

```bash
python3 -m http.server 8000
```

Run that command from the project root, not from inside `wasm/`.

Serving from the project root lets the browser load the files using these paths:

- `wasm/index.html`
- `wasm/runtime.js`
- `wasm/demo.wasm`

Then open:

```text
http://127.0.0.1:8000/wasm/
```

The page loads `demo.wasm`, instantiates it, and calls the exported functions from JavaScript.
