package app.ast;

import java.util.List;

public record While(Expr cond, List<Stmt> body) implements Stmt {}
