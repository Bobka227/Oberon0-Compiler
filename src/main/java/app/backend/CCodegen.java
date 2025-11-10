package app.backend;

import app.ast.*;
import java.util.*;
import java.util.stream.Collectors;

public final class CCodegen {

    private final String moduleName;
    private final StringBuilder out = new StringBuilder();

    private final Deque<String> procStack = new ArrayDeque<>();
    private final Deque<Map<String, TypeRef>> varScopes = new ArrayDeque<>();

    private final Map<String, String> topNames = new HashMap<>();

    private final Deque<Set<String>> nestedVisible = new ArrayDeque<>();

    public CCodegen(String moduleName) {
        this.moduleName = moduleName;
    }

    private void pushScope() {
        Map<String, TypeRef> base = varScopes.isEmpty() ? new HashMap<>() : new HashMap<>(varScopes.peek());
        varScopes.push(base);
    }

    private void popScope() {
        varScopes.pop();
    }

    private void declareVar(String name, TypeRef t) {
        if (varScopes.isEmpty()) {
            pushScope();
        }
        varScopes.peek().put(name, t);
    }

    private TypeRef lookupVar(String name) {
        for (Map<String, TypeRef> m : varScopes) {
            TypeRef t = m.get(name);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public String generate(Program p) {
        emit("#include <stdio.h>\n#include <string.h>\n#include <math.h>\n\n");
        pushScope();
        emit("static void __print_bool(int b){ printf(b?\"TRUE\":\"FALSE\"); }\n");
        emit("static void __read_bool(int* b){ char buf[8]; if (scanf(\"%7s\", buf)==1){ *b = (strcmp(buf,\"TRUE\")==0); } }\n\n");

        for (Decl d : p.decls()) {
            if (d instanceof ProcDecl pr) {
                String nm = mangleTop(pr.name());
                topNames.put(pr.name(), nm);
                emit("void ").append(nm).append("(").append(paramsProto(pr.params())).append(");\n");
            } else if (d instanceof FuncDecl fn) {
                String nm = mangleTop(fn.name());
                topNames.put(fn.name(), nm);
                emit(type(fn.retType())).append(" ").append(nm).append("(").append(paramsProto(fn.params())).append(");\n");
            }
        }
        if (!p.decls().isEmpty()) {
            emit("\n");
        }

        for (Decl d : p.decls()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type());
                emitVarDecl("", v.name(), v.type());
            }
        }
        if (!p.decls().isEmpty()) {
            emit("\n");
        }

        for (Decl d : p.decls()) {
            if (d instanceof ProcDecl pr) {
                emitProc(pr);
                emit("\n");
            } else if (d instanceof FuncDecl fn) {
                emitFunc(fn);
                emit("\n");
            }
        }

        emit("int main(void){\n");
        for (Stmt s : p.body()) {
            emit("  ").append(stmt(s)).append("\n");
        }
        emit("  return 0;\n}\n");

        return out.toString();
    }

    private void emitProc(ProcDecl pr) {
        procStack.push(pr.name());
        nestedVisible.push(collectNestedNames(pr.nested()));
        pushScope();
        for (Param par : pr.params()) {
            declareVar(par.name(), par.type());
        }

        String self = mangleTop(pr.name());
        emit("void ").append(self).append("(").append(paramsProto(pr.params())).append("){\n");

        for (Decl d : pr.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type());
                emitVarDecl("  ", v.name(), v.type());
            }
        }

        for (Decl d : pr.nested()) {
            if (d instanceof ProcDecl q) {
                emitNestedProc(q);
                emit("\n");
            } else if (d instanceof FuncDecl g) {
                emitNestedFunc(g);
                emit("\n");
            }
        }

        for (Stmt s : pr.body()) {
            emit("  ").append(stmt(s)).append("\n");
        }
        emit("}\n");

        popScope();
        nestedVisible.pop();
        procStack.pop();
    }

    private void emitFunc(FuncDecl fn) {
        procStack.push(fn.name());
        nestedVisible.push(collectNestedNames(fn.nested()));
        pushScope();
        for (Param par : fn.params()) {
            declareVar(par.name(), par.type());
        }

        String self = mangleTop(fn.name());
        emit(type(fn.retType())).append(" ").append(self).append("(").append(paramsProto(fn.params())).append("){\n");

        for (Decl d : fn.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type());
                emitVarDecl("  ", v.name(), v.type());
            }
        }

        for (Decl d : fn.nested()) {
            if (d instanceof ProcDecl q) {
                emitNestedProc(q);
                emit("\n");
            } else if (d instanceof FuncDecl g) {
                emitNestedFunc(g);
                emit("\n");
            }
        }

        for (Stmt s : fn.body()) {
            emit("  ").append(stmt(s)).append("\n");
        }
        emit("}\n");

        popScope();
        nestedVisible.pop();
        procStack.pop();
    }

    private void emitNestedProc(ProcDecl pr) {
        nestedVisible.push(collectNestedNames(pr.nested()));
        pushScope(); 
        for (Param par : pr.params()) {
            declareVar(par.name(), par.type()); 
        }
        emit("  void ").append(mangle(pr.name())).append("(").append(paramsProto(pr.params())).append("){\n");
        for (Decl d : pr.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type());
                emitVarDecl("    ", v.name(), v.type());
            }
        }
        for (Stmt s : pr.body()) {
            emit("    ").append(stmt(s)).append("\n");
        }
        emit("  }\n");
        popScope();
        nestedVisible.pop();
    }

    private void emitNestedFunc(FuncDecl fn) {
        nestedVisible.push(collectNestedNames(fn.nested()));
        pushScope();
        for (Param par : fn.params()) {
            declareVar(par.name(), par.type());
        }
        emit("  ").append(type(fn.retType())).append(" ").append(mangle(fn.name()))
                .append("(").append(paramsProto(fn.params())).append("){\n");
        for (Decl d : fn.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type());
                emitVarDecl("    ", v.name(), v.type());
            }
        }
        for (Stmt s : fn.body()) {
            emit("    ").append(stmt(s)).append("\n");
        }
        emit("  }\n");
        popScope();
        nestedVisible.pop();
    }
    
//    private String paramsProto(List<Param> ps) {
//        return ps.stream().map(p -> {
//            TypeRef t = p.type();
//            if (t instanceof ArrayType a) {
//                return typeRef(a.elementType()) + "* " + p.name();
//            }
//            if (t instanceof Type base) {
//                return type(base) + " " + p.name();
//            }
//            return "/*unknown*/ " + p.name();
//        }).collect(Collectors.joining(", "));
//    }
    
    private String paramsProto(List<Param> ps){
    return ps.stream().map(p -> {
        TypeRef t = p.type();
        if (t instanceof ArrayType) {
            Flat f = flatten(t);                
            List<Integer> dims = f.dims;

            String tail = dims.stream().skip(1)
                              .map(d -> "[" + d + "]")
                              .collect(Collectors.joining());

            if (dims.size() <= 1) {
                return baseTypeOf(t) + "* " + p.name();          
            } else {
                return baseTypeOf(t) + " (*" + p.name() + ")" + tail; 
            }
        }
        if (t instanceof Type base) {
            return type(base) + " " + p.name();
        }
        return "/*unknown*/ " + p.name();
    }).collect(Collectors.joining(", "));
}


    private String type(Type t) {
        return switch (t) {
            case INTEGER ->
                "int";
            case REAL ->
                "double";
            case BOOLEAN ->
                "int";
            case STRING ->
                "const char*";
        };
    }

    private String typeRef(TypeRef t) {
        if (t instanceof Type base) {
            return type(base);
        }
        if (t instanceof ArrayType) {
            return baseTypeOf(t);
        }
        return "/*unknown*/";
    }

    private void emitVarDecl(String indent, String name, TypeRef t) {
        if (t instanceof ArrayType) {
            emit(indent).append(baseTypeOf(t)).append(" ")
                    .append(name).append(dimsAll(t)).append(";\n");
        } else if (t instanceof Type base) {
            emit(indent).append(type(base)).append(" ").append(name).append(";\n");
        } else {
            emit(indent).append("/* unknown type */ ").append(name).append(";\n");
        }
    }

    private String dimsToString(List<Integer> dims) {
        return dims.stream().map(d -> "[" + d + "]").collect(Collectors.joining());
    }

    private String stmt(Stmt s) {
        if (s instanceof Assign a) {
            return a.name() + " = " + expr(a.value()) + ";";
        }
        if (s instanceof AssignIndex ai) {
            return expr(ai.target()) + " = " + expr(ai.value()) + ";";
        }
        if (s instanceof CallStmt c) {
            if (c.name().equals("write")) {
                return emitWrite(c.args(), false);
            }
            if (c.name().equals("writeln")) {
                return emitWrite(c.args(), true);
            }
            if (c.name().equals("read")) {
                if (c.args().isEmpty()) {
                    return "/* read() no args */;";
                }
                return emitRead(c.args().get(0)) + ";";
            }
            return mangleMaybe(c.name()) + "(" + c.args().stream().map(this::expr).collect(Collectors.joining(", ")) + ");";
        }
        if (s instanceof Return r) {
            return "return " + expr(r.value()) + ";";
        }
        if (s instanceof ReturnVoid) {
            return "return;";
        }
        if (s instanceof Break) {
            return "break;";
        }
        if (s instanceof Continue) {
            return "continue;";
        }
        if (s instanceof If iff) {
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(expr(iff.cond())).append(") {\n");
            for (Stmt t : iff.thenPart()) {
                sb.append("  ").append(stmt(t)).append("\n");
            }
            for (ElseIf e : iff.elseIfs()) {
                sb.append("} else if (").append(expr(e.cond())).append(") {\n");
                for (Stmt t : e.body()) {
                    sb.append("  ").append(stmt(t)).append("\n");
                }
            }
            if (!iff.elsePart().isEmpty()) {
                sb.append("} else {\n");
                for (Stmt t : iff.elsePart()) {
                    sb.append("  ").append(stmt(t)).append("\n");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        if (s instanceof While w) {
            return "while (" + expr(w.cond()) + ") " + block1(w.body());
        }
        if (s instanceof Repeat r) {
            StringBuilder sb = new StringBuilder("do {\n");
            for (Stmt t : r.body()) {
                sb.append("  ").append(stmt(t)).append("\n");
            }
            sb.append("} while (!(").append(expr(r.cond())).append("));");
            return sb.toString();
        }
        if (s instanceof For f) {
            String i = f.var();
            return "for (" + i + " = " + expr(f.from()) + "; " + i + " <= " + expr(f.to()) + "; " + i + "++) " + block1(f.body());
        }
        return "/* unknown stmt */;";
    }

    private String block1(List<Stmt> body) {
        if (body == null || body.isEmpty()) {
            return "{ }";
        }
        if (body.size() == 1) {
            String one = stmt(body.get(0));
            if (!one.contains("\n")) {
                return "{ " + one + " }";
            }
        }
        StringBuilder sb = new StringBuilder("{\n");
        for (Stmt s : body) {
            sb.append("  ").append(stmt(s)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private String expr(Expr e) {
        if (e instanceof IntLit i) {
            return Integer.toString(i.value());
        }
        if (e instanceof RealLit d) {
            return Double.toString(d.value());
        }
        if (e instanceof BoolLit b) {
            return b.value() ? "1" : "0";
        }
        if (e instanceof StringLit s) {
            return "\"" + s.value().replace("\"", "\\\"") + "\"";
        }
        if (e instanceof Var v) {
            return v.name();
        }
        if (e instanceof Paren p) {
            return "(" + expr(p.inner()) + ")";
        }
        if (e instanceof Unary u) {
            String op = switch (u.op()) {
                case POS ->
                    "+";
                case NEG ->
                    "-";
                default ->
                    "!";
            };
            return op + "(" + expr(u.value()) + ")";
        }
        if (e instanceof Binary b) {
            String op = switch (b.op()) {
                case ADD ->
                    "+";
                case SUB ->
                    "-";
                case MUL ->
                    "*";
                case DIV ->
                    "/";
                case MOD ->
                    "%";
                case AND ->
                    "&&";
                case OR ->
                    "||";
                case EQ ->
                    "==";
                case NE ->
                    "!=";
                case LT ->
                    "<";
                case LE ->
                    "<=";
                case GT ->
                    ">";
                case GE ->
                    ">=";
            };
            return "(" + expr(b.left()) + " " + op + " " + expr(b.right()) + ")";
        }
        if (e instanceof CallExpr c) {
            return mangleMaybe(c.name()) + "(" + c.args().stream().map(this::expr).collect(Collectors.joining(", ")) + ")";
        }
        if (e instanceof ArrayAccess a) {
            String base = expr(a.base());
            String idxs = a.indices().stream().map(this::expr).map(s -> "[" + s + "]").collect(Collectors.joining());
            return base + idxs;
        }
        return "/* unknown expr */";
    }

    private String emitWrite(List<Expr> args, boolean ln) {
        if (args.isEmpty()) {
            return ln ? "printf(\"\\n\");" : "/* write() */;";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            Expr x = args.get(i);
            switch (kindOf(x)) {
                case INT ->
                    sb.append("printf(\"%d\", ").append(expr(x)).append(");");
                case REAL ->
                    sb.append("printf(\"%g\", ").append(expr(x)).append(");");
                case BOOL ->
                    sb.append("__print_bool(").append(expr(x)).append(");");
                case STR ->
                    sb.append("printf(\"%s\", ").append(expr(x)).append(");");
                case ARRAY ->
                    sb.append("/* cannot print arrays */;");
            }
        }
        if (ln) {
            sb.append(" printf(\"\\n\");");
        }
        return sb.toString();
    }

    private String emitRead(Expr arg) {
        String addr;
        if (arg instanceof Var v) {
            addr = "&" + v.name();
        } else if (arg instanceof ArrayAccess a) {
            addr = "&" + expr(a);
        } else {
            return "/* invalid read target */";
        }

        switch (kindOf(arg)) {
            case INT:
                return "scanf(\"%d\", " + addr + ")";
            case REAL:
                return "scanf(\"%lf\", " + addr + ")"; 
            case BOOL:
                return "__read_bool(" + addr + ")";
            case STR:
                return "/* read(string) not supported: need char buffer */";
            default:
                return "/* cannot read arrays */";
        }
    }

    private enum K {
        INT, REAL, BOOL, STR, ARRAY
    }

    private K kindOf(Expr e) {
        if (e instanceof StringLit) {
            return K.STR;
        }
        if (e instanceof BoolLit) {
            return K.BOOL;
        }
        if (e instanceof RealLit) {
            return K.REAL;
        }
        if (e instanceof IntLit) {
            return K.INT;
        }

        if (e instanceof Var v) {
            TypeRef t = lookupVar(v.name());
            if (t instanceof Type bt) {
                return switch (bt) {
                    case STRING ->
                        K.STR;
                    case BOOLEAN ->
                        K.BOOL;
                    case REAL ->
                        K.REAL;
                    case INTEGER ->
                        K.INT;
                };
            }
            return K.ARRAY;
        }
        if (e instanceof ArrayAccess aa) {
            TypeRef et = elementTypeOf(aa);
            if (et instanceof Type bt) {
                return switch (bt) {
                    case STRING ->
                        K.STR;
                    case BOOLEAN ->
                        K.BOOL;
                    case REAL ->
                        K.REAL;
                    case INTEGER ->
                        K.INT;
                };
            }
            return K.ARRAY;
        }
        return K.INT;
    }

    private String mangleTop(String name) {
        return "__" + moduleName + "_" + name;
    }

    private String mangle(String name) {
        if (procStack.isEmpty()) {
            return mangleTop(name);
        }
        return "__" + moduleName + "_" + String.join("__", procStack) + "__" + name;
    }

    private Set<String> collectNestedNames(List<Decl> nested) {
        Set<String> s = new HashSet<>();
        for (Decl d : nested) {
            if (d instanceof ProcDecl q) {
                s.add(q.name());
            } else if (d instanceof FuncDecl g) {
                s.add(g.name());
            }
        }
        return s;
    }

    private String mangleMaybe(String name) {
        for (Iterator<Set<String>> it = nestedVisible.descendingIterator(); it.hasNext();) {
            if (it.next().contains(name)) {
                return mangle(name);
            }
        }
        return topNames.getOrDefault(name, name);
    }

    private static final class Flat {

        final Type base;
        final List<Integer> dims;

        Flat(Type base, List<Integer> dims) {
            this.base = base;
            this.dims = dims;
        }
    }

    private Flat flatten(TypeRef t) {
        if (t instanceof Type base) {
            return new Flat(base, List.of());
        }
        if (t instanceof ArrayType a) {
            Flat inner = flatten(a.elementType());
            List<Integer> all = new ArrayList<>(a.dimensions());
            all.addAll(inner.dims);
            return new Flat(inner.base, all);
        }
        return new Flat(Type.INTEGER, List.of());
    }

    private String baseTypeOf(TypeRef t) {
        return type(flatten(t).base);
    }

    private String dimsAll(TypeRef t) {
        return flatten(t).dims.stream().map(d -> "[" + d + "]").collect(Collectors.joining());
    }

    private StringBuilder emit(String s) {
        out.append(s);
        return out;
    }

    private int totalIndices(Expr e) {
        int c = 0;
        Expr cur = e;
        while (cur instanceof ArrayAccess aa) {
            c += aa.indices().size();
            cur = aa.base();
        }
        return c;
    }

    private Var baseVar(Expr e) {
        Expr cur = e;
        while (cur instanceof ArrayAccess aa) {
            cur = aa.base();
        }
        return (cur instanceof Var v) ? v : null;
    }

    private TypeRef elementTypeOf(ArrayAccess aa) {
        Var bv = baseVar(aa);
        TypeRef t = (bv != null) ? lookupVar(bv.name()) : null;
        if (t == null) {
            return Type.INTEGER;
        }
        List<Integer> dims = new ArrayList<>();
        TypeRef cur = t;
        while (cur instanceof ArrayType A) {
            dims.addAll(A.dimensions());
            cur = A.elementType();
        }
        int used = totalIndices(aa);
        if (used >= dims.size()) {
            return cur;
        } else {
            return new ArrayType(cur, dims.subList(used, dims.size()));
        }
    }

}
