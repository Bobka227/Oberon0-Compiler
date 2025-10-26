package app.sem;

import app.ast.TypeRef;
import app.ast.Type;
import java.util.List;

public sealed interface Symbol permits VarSym, ProcSym, FuncSym {
    String name();
}