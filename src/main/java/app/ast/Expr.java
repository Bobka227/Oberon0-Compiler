package app.ast;
public sealed interface Expr extends Ast permits IntLit, BoolLit, StringLit, RealLit, Var, Paren, Unary, Binary, CallExpr, ArrayAccess {
    default boolean isBool() { return this instanceof BoolLit; }
}
