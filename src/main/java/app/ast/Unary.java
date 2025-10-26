package app.ast;
public record Unary(UnOp op, Expr expr) implements Expr {
    public Expr value() { return expr; } 
}