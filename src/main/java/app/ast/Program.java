package app.ast;
import java.util.List;
public record Program(String name, List<Decl> decls, List<Stmt> body) implements Ast {}
