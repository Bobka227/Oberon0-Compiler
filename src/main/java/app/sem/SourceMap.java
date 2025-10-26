package app.sem;

import org.antlr.v4.runtime.ParserRuleContext;
import java.util.IdentityHashMap;
import java.util.Map;
import org.antlr.v4.runtime.Token;

public final class SourceMap {

    private final String file;
    private final Map<Object, Span> map = new IdentityHashMap<>();

    public SourceMap(String file) {
        this.file = file;
    }

    public SourceMap() {
        this(null);
    }

    public void put(Object astNode, ParserRuleContext ctx) {
        Token t = ctx.getStart();
        map.put(astNode, new Span(file != null ? file : "unknown",
                t.getLine(), t.getCharPositionInLine() + 1));
    }

    public void put(Object astNode, Token tok) {
        map.put(astNode, new Span(file != null ? file : "unknown",
                tok.getLine(), tok.getCharPositionInLine() + 1));
    }

    public Span get(Object astNode) {
        return map.getOrDefault(astNode,
                new Span(file != null ? file : "unknown", 1, 1));
    }
}
