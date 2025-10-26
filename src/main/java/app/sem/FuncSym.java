package app.sem;

import app.ast.TypeRef;
import app.ast.Type;
import java.util.List;

public record FuncSym(String name, List<TypeRef> paramTypes, TypeRef returnType) implements Symbol {}
