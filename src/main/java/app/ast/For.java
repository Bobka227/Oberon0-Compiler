package app.ast;

import java.util.List;

public record For(String var, Expr from, Expr to, List<Stmt> body) implements Stmt {}
