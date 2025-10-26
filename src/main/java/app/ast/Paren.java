package app.ast;
public record Paren(Expr inner) implements Expr {}
