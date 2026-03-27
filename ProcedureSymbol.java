import java.util.HashMap;
import java.util.Map;
import my.delphi.delphiParser;

public class ProcedureSymbol {
    public delphiParser.ProcedureDeclarationContext ctx;
    public Environment definitionEnv;

    public ProcedureSymbol(delphiParser.ProcedureDeclarationContext ctx, Environment definitionEnv) {
        this.ctx = ctx;
        this.definitionEnv = definitionEnv;
    }
}