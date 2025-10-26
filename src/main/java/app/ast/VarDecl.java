// app/ast/VarDecl.java
package app.ast;
public record VarDecl(String name, TypeRef type) implements Decl {}
