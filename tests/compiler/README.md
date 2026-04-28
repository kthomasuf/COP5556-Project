# Compiler Tests

These tests target LLVM compile mode rather than the tree-walking interpreter.

Run one test with:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/compiler/core_arithmetic_io.pas --compile
clang output.ll -o output
./output
```

`class_private_field_rejected.pas` is expected to fail during code generation because it accesses a private field from outside the class.

Run the full compiler suite with:

```bash
bash run_compiler_tests.sh
```

Current coverage includes:

- arithmetic / comparison / boolean expressions
- `WriteLn` / `ReadLn`
- control flow
- global procedures / functions
- class fields / methods / constructors / destructors
- private visibility checks

`wasm_export_demo.pas` is a small no-I/O sample intended for the WebAssembly extra-credit build path.
