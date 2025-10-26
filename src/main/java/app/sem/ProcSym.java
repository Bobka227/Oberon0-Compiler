package app.sem;

import app.ast.TypeRef;
import app.ast.Type;
import java.util.List;

public record ProcSym(String name, List<TypeRef> paramTypes) implements Symbol {}
