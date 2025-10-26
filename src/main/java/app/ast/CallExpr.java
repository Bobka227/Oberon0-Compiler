package app.ast;
import java.util.List;
public record CallExpr(String name, List<Expr> args) implements Expr {}
