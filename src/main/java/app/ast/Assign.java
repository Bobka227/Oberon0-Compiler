package app.ast;
public record Assign(String name, Expr value) implements Stmt {}
