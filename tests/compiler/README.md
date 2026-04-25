# Compiler Tests

These tests target LLVM compile mode rather than the tree-walking interpreter.

Run one test with:

```bash
java -cp ./src:lib/antlr-4.13.2-complete.jar Main tests/compiler/core_arithmetic_io.pas --compile
clang output.ll -o output
./output
```

`class_private_field_rejected.pas` is expected to fail during code generation because it accesses a private field from outside the class.
