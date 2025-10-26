package app.frontend;

import app.ast.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Печатает AST в виде «похожем на код»
 */
public class AstPrinter {

    public String print(Program p) {
        StringBuilder sb = new StringBuilder();
        sb.append("module ").append(p.name()).append(";\n");

        if (!p.decls().isEmpty()) {
            // var-секция (глобалы)
            var vars = p.decls().stream()
                    .filter(d -> d instanceof VarDecl)
                    .map(d -> (VarDecl) d)
                    .toList();
            if (!vars.isEmpty()) {
                sb.append("var\n");
                for (VarDecl v : vars) {
                    sb.append(indent(1))
                            .append(v.name()).append(" : ")
                            .append(typeRef(v.type())).append(";\n");
                }
            }
            // процедуры/функции верхнего уровня
            for (Decl d : p.decls()) {
                if (d instanceof ProcDecl pr) {
                    sb.append(printProc(pr, 0)).append("\n");
                } else if (d instanceof FuncDecl fn) {
                    sb.append(printFunc(fn, 0)).append("\n");
                }
            }
        }

        sb.append("begin\n");
        for (Stmt s : p.body()) {
            sb.append(indent(1)).append(stmt(s, 1)).append("\n");
        }
        sb.append("end ").append(p.name()).append(".");
        return sb.toString();
    }

    // ================= helpers =================
    private static String indent(int n) {
        return "  ".repeat(n);
    }

    private String showTypeRef(TypeRef t) {
        if (t instanceof Type base) {
            return type(base);
        }
        if (t instanceof ArrayType a) {
            return showTypeRef(a.elementType()) + dimsToString(a.dimensions());
        }
        return "<?>";
    }

    private String dimsToString(List<Integer> dims) {
        return dims.stream().map(d -> "[" + d + "]").collect(Collectors.joining());
    }

    private String params(List<Param> ps) {
        return ps.stream()
                .map(p -> p.name() + " : " + showTypeRef(p.type()))
                .collect(Collectors.joining(", "));
    }

    // ================= procedures / functions =================
    private String printProc(ProcDecl pr, int ind) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(ind))
                .append("procedure ").append(pr.name())
                .append("(").append(params(pr.params())).append(");\n");

        var vars = pr.locals().stream()
                .filter(d -> d instanceof VarDecl)
                .map(d -> (VarDecl) d)
                .toList();
        if (!vars.isEmpty()) {
            sb.append(indent(ind)).append("var\n");
            for (VarDecl v : vars) {
                sb.append(indent(ind + 1))
                        .append(v.name()).append(" : ")
                        .append(typeRef(v.type())).append(";\n");
            }
        }

        // nested procs/funcs
        for (Decl d : pr.nested()) {
            if (d instanceof ProcDecl p) {
                sb.append(printProc(p, ind)).append("\n");
            } else if (d instanceof FuncDecl f) {
                sb.append(printFunc(f, ind)).append("\n");
            }
        }

        // body
        sb.append(indent(ind)).append("begin\n");
        for (Stmt s : pr.body()) {
            sb.append(indent(ind + 1)).append(stmt(s, ind + 1)).append("\n");
        }
        sb.append(indent(ind)).append("end ").append(pr.name()).append(";");
        return sb.toString();
    }

    private String printFunc(FuncDecl fn, int ind) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(ind))
                .append("function ").append(fn.name())
                .append("(").append(params(fn.params())).append(")")
                .append(" : ").append(type(fn.retType())).append(";\n");

        // locals
        var vars = fn.locals().stream()
                .filter(d -> d instanceof VarDecl)
                .map(d -> (VarDecl) d)
                .toList();
        if (!vars.isEmpty()) {
            sb.append(indent(ind)).append("var\n");
            for (VarDecl v : vars) {
                sb.append(indent(ind + 1))
                        .append(v.name()).append(" : ")
                        .append(typeRef(v.type())).append(";\n");
            }
        }

        // nested procs/funcs
        for (Decl d : fn.nested()) {
            if (d instanceof ProcDecl p) {
                sb.append(printProc(p, ind)).append("\n");
            } else if (d instanceof FuncDecl f) {
                sb.append(printFunc(f, ind)).append("\n");
            }
        }

        // body
        sb.append(indent(ind)).append("begin\n");
        for (Stmt s : fn.body()) {
            sb.append(indent(ind + 1)).append(stmt(s, ind + 1)).append("\n");
        }
        sb.append(indent(ind)).append("end ").append(fn.name()).append(";");
        return sb.toString();
    }

    // ================= statements =================
    private String stmt(Stmt s, int ind) {
        if (s instanceof Assign a) {
            return a.name() + " := " + expr(a.value()) + ";";
        }
        if (s instanceof AssignIndex ai) {
            return expr(ai.target()) + " := " + expr(ai.value()) + ";";
        }
        if (s instanceof CallStmt c) {
            return c.name() + "(" + c.args().stream().map(this::expr).collect(Collectors.joining(", ")) + ");";
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
            sb.append("if ").append(expr(iff.cond())).append(" then\n");
            for (Stmt t : iff.thenPart()) {
                sb.append(indent(ind + 1)).append(stmt(t, ind + 1)).append("\n");
            }
            for (ElseIf e : iff.elseIfs()) {
                sb.append(indent(ind - 0)).append("elseif ").append(expr(e.cond())).append(" then\n");
                for (Stmt t : e.body()) {
                    sb.append(indent(ind + 1)).append(stmt(t, ind + 1)).append("\n");
                }
            }
            if (!iff.elsePart().isEmpty()) {
                sb.append(indent(ind - 0)).append("else\n");
                for (Stmt t : iff.elsePart()) {
                    sb.append(indent(ind + 1)).append(stmt(t, ind + 1)).append("\n");
                }
            }
            sb.append(indent(ind - 0)).append("end;");
            return sb.toString();
        }
        if (s instanceof While w) {
            return "while " + expr(w.cond()) + " do " + oneLineOrBlock(w.body(), ind);
        }
        if (s instanceof Repeat r) {
            StringBuilder sb = new StringBuilder("repeat\n");
            for (Stmt t : r.body()) {
                sb.append(indent(ind + 1)).append(stmt(t, ind + 1)).append("\n");
            }
            sb.append("until ").append(expr(r.cond())).append(";");
            return sb.toString();
        }
        if (s instanceof For f) {
            return "for " + f.var() + " := " + expr(f.from()) + " to " + expr(f.to())
                    + " do " + oneLineOrBlock(f.body(), ind);
        }
        return s.toString();
    }

    private String oneLineOrBlock(Stmt s, int ind) {
        String body = stmt(s, ind + 1);
        // если внутри есть перевод строки — печатаем как блок
        if (body.contains("\n")) {
            return "begin\n" + indent(ind + 1) + body + "\n" + indent(ind) + "end;";
        }
        return body;
    }

    private String oneLineOrBlock(List<Stmt> body, int ind) {
        if (body == null || body.isEmpty()) {
            return "begin\n" + indent(ind + 1) + "// empty\n" + indent(ind) + "end;";
        }
        if (body.size() == 1) {
            String only = stmt(body.get(0), ind + 1);
            // если внутри нет перевода строки — считаем однострочной
            if (!only.contains("\n")) {
                return only;
            }
        }
        StringBuilder sb = new StringBuilder("begin\n");
        for (Stmt t : body) {
            sb.append(indent(ind + 1)).append(stmt(t, ind + 1)).append("\n");
        }
        sb.append(indent(ind)).append("end;");
        return sb.toString();
    }

    // ================= expressions =================
    private String expr(Expr e) {
        if (e instanceof IntLit i) {
            return Integer.toString(i.value());
        }
        if (e instanceof RealLit d) {
            return Double.toString(d.value());
        }
        if (e instanceof BoolLit b) {
            return b.value() ? "TRUE" : "FALSE";
        }
        if (e instanceof StringLit s) {
            return "\"" + s.value() + "\"";
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
                    "not";
            };
            return op + " " + expr(u.value());
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
                    "mod";
                case AND ->
                    "and";
                case OR ->
                    "or";
                case EQ ->
                    "=";
                case NE ->
                    "#";
                case LT ->
                    "<";
                case LE ->
                    "<=";
                case GT ->
                    ">";
                case GE ->
                    ">=";
            };
            return expr(b.left()) + " " + op + " " + expr(b.right());
        }
        if (e instanceof CallExpr c) {
            return c.name() + "(" + c.args().stream().map(this::expr).collect(Collectors.joining(", ")) + ")";
        }
        if (e instanceof ArrayAccess a) {
            return expr(a.base()) + "[" + a.indices().stream().map(this::expr).collect(Collectors.joining(", ")) + "]";
        }
        return e.toString();
    }

    // ================= types =================
    private String typeRef(TypeRef t) {
        if (t instanceof Type base) {
            return type(base);
        }
        if (t instanceof ArrayType a) {
            return "array["
                    + a.dimensions().stream().map(Object::toString).collect(Collectors.joining(", "))
                    + "] of " + typeRef(a.elementType());
        }
        return t.toString();
    }

    private String type(Type t) {
        return switch (t) {
            case INTEGER ->
                "integer";
            case BOOLEAN ->
                "boolean";
            case REAL ->
                "real";
            case STRING ->
                "string";
        };
    }
}
