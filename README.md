# COP5556-Project

## Generate Lexer and Parser

```bash
java -jar antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -package my.delphi -o my/delphi delphi.g4
```

## Compile Interpreter Code

```bash
javac -cp .:antlr-4.13.2-complete.jar Main.java Value.java Instance.java Environment.java ClassSymbol.java my/delphi/*.java
```

## Run The Interpreter

Modify test.pas to execute different code

```bash
java -cp .:antlr-4.13.2-complete.jar Main test.pas
```


