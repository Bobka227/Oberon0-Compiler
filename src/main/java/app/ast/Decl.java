package app.ast;
public sealed interface Decl extends Ast permits VarDecl, ProcDecl, FuncDecl {}
