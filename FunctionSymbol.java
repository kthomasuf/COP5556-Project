import java.util.HashMap;
import java.util.Map;
import my.delphi.delphiParser;

public class FunctionSymbol {
    public delphiParser.FunctionDeclarationContext ctx;
    public Environment definitionEnv;

    public FunctionSymbol(delphiParser.FunctionDeclarationContext ctx, Environment definitionEnv) {
        this.ctx = ctx;
        this.definitionEnv = definitionEnv;
    }
}