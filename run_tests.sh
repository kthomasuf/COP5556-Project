#!/usr/bin/env bash
# run_tests.sh (auomated test runner)
# just run:  bash run_tests.sh

PASS=0; FAIL=0; SKIP=0; XFAIL=0
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

# find java, works on macOS, Linux, and Windows (Git Bash / WSL)
if command -v java &>/dev/null; then
    JAVA=java
elif [[ -n "$JAVA_HOME" ]]; then
    JAVA="$JAVA_HOME/bin/java"
else
    echo "java not found, make sure it's on your PATH"
    exit 1
fi

# separator character for -cp differs on Windows vs Unix
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OS" == "Windows_NT" ]]; then
    SEP=";"
else
    SEP=":"
fi

CP="./src${SEP}lib/antlr-4.13.2-complete.jar"

# tests that need simulated stdin, format: "testname:input_value"
# (using a plain list so this works on bash 3 / macOS default shell too)
INPUT_TESTS=(
    "test8:42"       # ReadLn: just echoes back whatever you give it
    "test12:250"     # ReadLn: deposit amount for the bank account test
)

# helper, look up simulated input for a test name, echoes empty string if not found
get_input() {
    local name="$1"
    for entry in "${INPUT_TESTS[@]}"; do
        if [[ "${entry%%:*}" == "$name" ]]; then
            echo "${entry#*:}"
            return
        fi
    done
}

is_input_test() {
    local name="$1"
    for entry in "${INPUT_TESTS[@]}"; do
        [[ "${entry%%:*}" == "$name" ]] && return 0
    done
    return 1
}

# tests that are supposed to blow up, we verify they do
# test6, tries to write a private field from outside the class
# test7, tries to call a private method from outside the class
# test24, BREAK with no loop around it
# test25, CONTINUE with no loop around it
CRASH_TESTS=(test6 test7 test24 test25)

contains() { for e in "${@:2}"; do [[ "$e" == "$1" ]] && return 0; done; return 1; }

printf "\n========================================\n"
printf "    Delphi Interpreter Test Suite\n"
printf "========================================\n\n"

for test_file in $(find tests -maxdepth 1 -name 'test*.pas' | sort -V); do
    name=$(basename "$test_file" .pas)
    expected_file="tests/expected/${name}.txt"

    # simulated-input tests
    if is_input_test "$name"; then
        sim_input=$(get_input "$name")
        if [[ ! -f "$expected_file" ]]; then
            printf "  %-10s  ${YELLOW}SKIP${NC}   (no expected output file)\n" "$name"
            SKIP=$((SKIP+1))
            continue
        fi
        actual=$(echo "$sim_input" | "$JAVA" -cp "$CP" Main "$test_file" 2>/dev/null \
                 | grep '^>> OUTPUT:' | sed 's/^>> OUTPUT: //')
        expected=$(cat "$expected_file")
        if [[ "$actual" == "$expected" ]]; then
            printf "  %-10s  ${GREEN}PASS${NC}   (input: ${sim_input})\n" "$name"
            PASS=$((PASS+1))
        else
            printf "  %-10s  ${RED}FAIL${NC}   (input: ${sim_input})\n" "$name"
            diff <(echo "$expected") <(echo "$actual") | sed 's/^/    /'
            FAIL=$((FAIL+1))
        fi
        continue
    fi

    # tests that should crash
    if contains "$name" "${CRASH_TESTS[@]}"; then
        "$JAVA" -cp "$CP" Main "$test_file" > /dev/null 2>&1
        if [[ $? -ne 0 ]]; then
            printf "  %-10s  ${CYAN}XFAIL${NC}  (crashed as expected)\n" "$name"
            XFAIL=$((XFAIL+1))
        else
            printf "  %-10s  ${RED}FAIL${NC}   (expected a crash but it ran fine, check the test)\n" "$name"
            FAIL=$((FAIL+1))
        fi
        continue
    fi

    # no expected file yet, skip gracefully
    if [[ ! -f "$expected_file" ]]; then
        printf "  %-10s  ${YELLOW}SKIP${NC}   (no expected output file)\n" "$name"
        SKIP=$((SKIP+1))
        continue
    fi

    # normal pass/fail comparison
    actual=$("$JAVA" -cp "$CP" Main "$test_file" 2>/dev/null \
             | grep '^>> OUTPUT:' | sed 's/^>> OUTPUT: //')
    expected=$(cat "$expected_file")

    if [[ "$actual" == "$expected" ]]; then
        printf "  %-10s  ${GREEN}PASS${NC}\n" "$name"
        PASS=$((PASS+1))
    else
        printf "  %-10s  ${RED}FAIL${NC}\n" "$name"
        diff <(echo "$expected") <(echo "$actual") | sed 's/^/    /'
        FAIL=$((FAIL+1))
    fi
done

printf "\n========================================\n"
printf "  ${GREEN}PASS: %d${NC}  ${RED}FAIL: %d${NC}  ${CYAN}XFAIL: %d${NC}  ${YELLOW}SKIP: %d${NC}\n" \
    $PASS $FAIL $XFAIL $SKIP
printf "========================================\n\n"

[[ $FAIL -eq 0 ]]
