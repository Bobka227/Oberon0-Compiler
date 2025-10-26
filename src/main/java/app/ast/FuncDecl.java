package app.ast;
import java.util.List;

public record FuncDecl(String name, List<Param> params, Type retType,
                       List<Decl> locals, List<Decl> nested, List<Stmt> body) implements Decl {}
