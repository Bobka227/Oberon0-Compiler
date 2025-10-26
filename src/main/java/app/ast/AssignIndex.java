// app/ast/AssignIndex.java
package app.ast;
import java.util.List;
public record AssignIndex(ArrayAccess target, Expr value) implements Stmt { }
