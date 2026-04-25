#!/usr/bin/env bash
# run_compiler_tests.sh

PASS=0; FAIL=0; XFAIL=0; SKIP=0
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

if command -v java &>/dev/null; then
    JAVA=java
else
    echo "java not found, make sure it's on your PATH"
    exit 1
fi

if command -v javac &>/dev/null; then
    JAVAC=javac
else
    echo "javac not found, make sure it's on your PATH"
    exit 1
fi

if command -v clang &>/dev/null; then
    CLANG=clang
else
    echo "clang not found, skipping native LLVM validation"
    CLANG=
fi

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OS" == "Windows_NT" ]]; then
    SEP=";"
else
    SEP=":"
fi

CP="./src${SEP}lib/antlr-4.13.2-complete.jar"

COMPILE_SOURCES=(
    src/Main.java
    src/Value.java
    src/Instance.java
    src/Environment.java
    src/ClassSymbol.java
    src/BreakException.java
    src/ContinueException.java
    src/ProcedureSymbol.java
    src/FunctionSymbol.java
    src/DelphiInterpreter.java
    src/LLVMCodeGenerator.java
    src/my/delphi/*.java
)

XFAIL_TESTS=(class_private_field_rejected)

contains() {
    local needle="$1"
    shift
    for item in "$@"; do
        [[ "$item" == "$needle" ]] && return 0
    done
    return 1
}

expected_output() {
    case "$1" in
        core_arithmetic_io)
            printf "11\n12.500000\n1"
            ;;
        readln_integer)
            printf "42"
            ;;
        class_public_field)
            printf "42"
            ;;
        class_inherited_fields)
            printf "12"
            ;;
        control_break_continue)
            printf "8"
            ;;
        control_for_loop)
            printf "10"
            ;;
        control_if_else)
            printf "1"
            ;;
        control_while_loop)
            printf "10"
            ;;
    esac
}

test_input() {
    case "$1" in
        readln_integer)
            printf "41\n"
            ;;
    esac
}

printf "\n========================================\n"
printf "    LLVM Compiler Test Suite\n"
printf "========================================\n\n"

"$JAVAC" -cp ".${SEP}lib/antlr-4.13.2-complete.jar" "${COMPILE_SOURCES[@]}"
if [[ $? -ne 0 ]]; then
    printf "${RED}java compile failed${NC}\n"
    exit 1
fi

for test_file in $(find tests/compiler -maxdepth 1 -name '*.pas' | sort); do
    name=$(basename "$test_file" .pas)

    if contains "$name" "${XFAIL_TESTS[@]}"; then
        "$JAVA" -cp "$CP" Main "$test_file" --compile > /tmp/compiler_test_compile.log 2>&1
        if [[ $? -ne 0 ]]; then
            printf "  %-30s ${CYAN}XFAIL${NC}  (failed as expected)\n" "$name"
            XFAIL=$((XFAIL+1))
        else
            printf "  %-30s ${RED}FAIL${NC}   (expected compile failure)\n" "$name"
            FAIL=$((FAIL+1))
        fi
        continue
    fi

    expected=$(expected_output "$name")
    if [[ -z "$expected" ]]; then
        printf "  %-30s ${YELLOW}SKIP${NC}   (no expected output)\n" "$name"
        SKIP=$((SKIP+1))
        continue
    fi

    "$JAVA" -cp "$CP" Main "$test_file" --compile > /tmp/compiler_test_compile.log 2>&1
    if [[ $? -ne 0 ]]; then
        printf "  %-30s ${RED}FAIL${NC}   (--compile failed)\n" "$name"
        sed 's/^/    /' /tmp/compiler_test_compile.log
        FAIL=$((FAIL+1))
        continue
    fi

    if [[ -n "$CLANG" ]]; then
        binary="/tmp/compiler_${name}"
        "$CLANG" output.ll -o "$binary" > /tmp/compiler_test_clang.log 2>&1
        if [[ $? -ne 0 ]]; then
            printf "  %-30s ${RED}FAIL${NC}   (clang rejected output.ll)\n" "$name"
            sed 's/^/    /' /tmp/compiler_test_clang.log
            FAIL=$((FAIL+1))
            continue
        fi

        input=$(test_input "$name")
        if [[ -n "$input" ]]; then
            actual=$(printf "%s" "$input" | "$binary")
        else
            actual=$("$binary")
        fi

        if [[ "$actual" == "$expected" ]]; then
            printf "  %-30s ${GREEN}PASS${NC}\n" "$name"
            PASS=$((PASS+1))
        else
            printf "  %-30s ${RED}FAIL${NC}   (wrong output)\n" "$name"
            diff <(printf "%s\n" "$expected") <(printf "%s\n" "$actual") | sed 's/^/    /'
            FAIL=$((FAIL+1))
        fi
    else
        printf "  %-30s ${YELLOW}SKIP${NC}   (clang unavailable)\n" "$name"
        SKIP=$((SKIP+1))
    fi
done

printf "\n========================================\n"
printf "  ${GREEN}PASS: %d${NC}  ${RED}FAIL: %d${NC}  ${CYAN}XFAIL: %d${NC}  ${YELLOW}SKIP: %d${NC}\n" \
    $PASS $FAIL $XFAIL $SKIP
printf "========================================\n\n"

[[ $FAIL -eq 0 ]]
