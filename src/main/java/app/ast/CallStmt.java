package app.ast;
import java.util.List;
public record CallStmt(String name, java.util.List<Expr> args) implements Stmt {}
