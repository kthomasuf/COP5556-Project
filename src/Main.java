import org.antlr.v4.runtime.*;
import my.delphi.*;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp .:antlr-4.13.2-complete.jar Main <file.pas> [--compile]");
            return;
        }

        boolean compileMode = args.length >= 2 && args[1].equals("--compile");

        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            delphiLexer lexer = new delphiLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            delphiParser parser = new delphiParser(tokens);

            if (compileMode) {
                LLVMCodeGenerator generator = new LLVMCodeGenerator();
                generator.visit(parser.program());
                // generator.writeToFile("output.ll");
            }
            else {
                DelphiInterpreter interpreter = new DelphiInterpreter();
                interpreter.visit(parser.program());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}