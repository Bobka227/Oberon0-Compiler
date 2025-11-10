package app.ast;
import java.util.List;
public record Repeat(List<Stmt> body, Expr until) implements Stmt {
    public Expr cond() { return until; } 
}