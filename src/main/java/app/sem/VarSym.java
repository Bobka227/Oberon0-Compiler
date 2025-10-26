package app.sem;

import app.ast.TypeRef;
import app.ast.Type;
import java.util.List;

public record VarSym(String name, TypeRef type, boolean isParam) implements Symbol {}
