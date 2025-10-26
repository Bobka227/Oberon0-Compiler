package app.ast;
import java.util.List;
public record ProcDecl(String name, List<Param> params,
                       List<Decl> locals, List<Decl> nested, List<Stmt> body) implements Decl {}
