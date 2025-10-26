// app/ast/ArrayAccess.java
package app.ast;
import java.util.List;
public record ArrayAccess(Expr base, List<Expr> indices) implements Expr { }
