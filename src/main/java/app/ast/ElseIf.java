package app.ast;
import java.util.List;
public record ElseIf(Expr cond, List<Stmt> body) {}
