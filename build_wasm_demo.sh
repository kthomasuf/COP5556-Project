#!/usr/bin/env bash

set -euo pipefail

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "${OS:-}" == "Windows_NT" ]]; then
    SEP=";"
else
    SEP=":"
fi

CP="./src${SEP}lib/antlr-4.13.2-complete.jar"
LLC_BIN="${LLC_BIN:-$(command -v llc || true)}"

find_wasm_ld() {
    if command -v wasm-ld >/dev/null 2>&1; then
        command -v wasm-ld
        return 0
    fi

    if command -v ld.lld >/dev/null 2>&1; then
        command -v ld.lld
        return 0
    fi

    for candidate in /opt/homebrew/opt/llvm/bin/wasm-ld /usr/local/opt/llvm/bin/wasm-ld; do
        if [[ -x "$candidate" ]]; then
            printf "%s\n" "$candidate"
            return 0
        fi
    done

    return 1
}

if [[ -z "$LLC_BIN" ]]; then
    echo "llc not found. install llvm tools first."
    exit 1
fi

mkdir -p wasm

javac -cp ".${SEP}lib/antlr-4.13.2-complete.jar" \
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

java -cp "$CP" Main wasm/wasm_export_demo.pas --compile
cp output.ll wasm/output.ll

"$LLC_BIN" -march=wasm32 -filetype=obj wasm/output.ll -o wasm/demo.o
echo "wrote wasm/demo.o"

if WASM_LD_BIN="$(find_wasm_ld)"; then
    "$WASM_LD_BIN" \
        --no-entry \
        --export=AddOne \
        --export=MaxValue \
        --export=SumToN \
        --export=CounterDemo \
        --export=main \
        --allow-undefined \
        wasm/demo.o \
        -o wasm/demo.wasm
    echo "wrote wasm/demo.wasm"
else
    echo "warning: wasm-ld not found, so final wasm linking was skipped"
    echo "install llvm linker tools, then rerun this script to generate wasm/demo.wasm"
fi
