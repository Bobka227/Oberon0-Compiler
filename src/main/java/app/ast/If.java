package app.ast;
import java.util.List;
public record If(Expr cond, List<Stmt> thenPart, List<ElseIf> elseIfs, List<Stmt> elsePart) implements Stmt {}
