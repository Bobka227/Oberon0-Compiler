package app.ast;
public sealed interface Stmt extends Ast permits Assign, If, While, Repeat, For, CallStmt, Return, ReturnVoid, Break, Continue,AssignIndex {}
