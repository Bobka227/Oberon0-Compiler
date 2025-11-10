package app.frontend;

import app.ast.*;
import java.util.*;

public class AstAsciiPrinter {

    private final StringBuilder out = new StringBuilder();

    public String print(Program p) {
        out.setLength(0);
        line("module " + p.name());

        if (p.decls() != null && !p.decls().isEmpty()) {
            section(p.decls(), "declarations", this::ppDecl);
        }
        if (p.body() != null && !p.body().isEmpty()) {
            section(p.body(), "statement sequence", this::ppStmt);
        }
        return out.toString();
    }


    private interface NodePrinter<T> { void print(T n, String pre, boolean last); }

    private <T> void section(List<T> items, String title, NodePrinter<T> pp) {
        line(title);
        forEach(items, "", pp);
    }

    private void line(String s) { out.append(s).append('\n'); }

    private <T> void forEach(List<T> items, String pre, NodePrinter<T> pp) {
        for (int i = 0; i < items.size(); i++) {
            boolean last = (i == items.size() - 1);
            out.append(pre).append(last ? "└── " : "├── ");
            pp.print(items.get(i), pre + (last ? "    " : "│   "), last);
        }
    }

    /* Decls */

    private void ppDecl(Decl d, String pre, boolean last) {
        if (d instanceof VarDecl v) {
            line("var " + v.name() + " : " + v.type());
            return;
        }
        if (d instanceof ProcDecl pr) {
            line("procedure " + pr.name());
            if (!pr.params().isEmpty()) {
                out.append(pre).append("├── params\n");
                forEach(pr.params(), pre + "│   ", this::ppParam);
            }
            if (pr.locals() != null && !pr.locals().isEmpty()) {
                out.append(pre).append("├── locals\n");
                forEach(pr.locals(), pre + "│   ", this::ppDecl);
            }
            if (pr.nested() != null && !pr.nested().isEmpty()) {
                out.append(pre).append("├── nested\n");
                forEach(pr.nested(), pre + "│   ", this::ppDecl);
            }
            out.append(pre).append("└── body\n");
            forEach(pr.body(), pre + "    ", this::ppStmt);
            return;
        }
        if (d instanceof FuncDecl f) {
            line("function " + f.name() + " : " + f.retType());
            if (!f.params().isEmpty()) {
                out.append(pre).append("├── params\n");
                forEach(f.params(), pre + "│   ", this::ppParam);
            }
            if (f.locals() != null && !f.locals().isEmpty()) {
                out.append(pre).append("├── locals\n");
                forEach(f.locals(), pre + "│   ", this::ppDecl);
            }
            if (f.nested() != null && !f.nested().isEmpty()) {
                out.append(pre).append("├── nested\n");
                forEach(f.nested(), pre + "│   ", this::ppDecl);
            }
            out.append(pre).append("└── body\n");
            forEach(f.body(), pre + "    ", this::ppStmt);
            return;
        }
        line(d.getClass().getSimpleName());
    }

    private void ppParam(Param p, String pre, boolean last) {
        line(p.name() + " : " + p.type());
    }


    private void ppStmt(Stmt s, String pre, boolean last) {
        if (s instanceof Assign a) {
            line("assign");
            out.append(pre).append("├── lhs ").append(a.name()).append('\n');
            out.append(pre).append("└── rhs\n");
            ppExpr(a.value(), pre + "    ", true);
            return;
        }
        if (s instanceof AssignIndex ai) {
            line("assign-index");
            out.append(pre).append("├── target\n");
            ppExpr(ai.target(), pre + "│   ", true);
            out.append(pre).append("└── rhs\n");
            ppExpr(ai.value(), pre + "    ", true);
            return;
        }
        if (s instanceof If iff) {
            line("branch");
            out.append(pre).append("├── condition\n");
            ppExpr(iff.cond(), pre + "│   ", true);

            out.append(pre).append("├── if-body\n");
            forEach(iff.thenPart(), pre + "│   ", this::ppStmt);

            if (iff.elseIfs() != null && !iff.elseIfs().isEmpty()) {
                out.append(pre).append("├── elsif\n");
                for (ElseIf ei : iff.elseIfs()) {
                    out.append(pre).append("│   ├── cond\n");
                    ppExpr(ei.cond(), pre + "│   │   ", true);
                    out.append(pre).append("│   └── body\n");
                    forEach(ei.body(), pre + "│   │   ", this::ppStmt);
                }
            }

            List<Stmt> elsePart = iff.elsePart() != null ? iff.elsePart() : List.of();
            if (!elsePart.isEmpty()) {
                out.append(pre).append("└── else-body\n");
                forEach(elsePart, pre + "    ", this::ppStmt);
            }
            return;
        }
        if (s instanceof While w) {
            line("while");
            out.append(pre).append("├── condition\n");
            ppExpr(w.cond(), pre + "│   ", true);
            out.append(pre).append("└── body\n");
            forEach(w.body(), pre + "    ", this::ppStmt);
            return;
        }
        if (s instanceof Repeat r) {
            line("repeat-until");
            out.append(pre).append("├── body\n");
            forEach(r.body(), pre + "│   ", this::ppStmt);
            out.append(pre).append("└── until\n");
            ppExpr(r.until(), pre + "    ", true);
            return;
        }
        if (s instanceof For f) {
            line("for " + f.var());
            out.append(pre).append("├── from\n");
            ppExpr(f.from(), pre + "│   ", true);
            out.append(pre).append("├── to\n");
            ppExpr(f.to(), pre + "│   ", true);
            out.append(pre).append("└── body\n");
            forEach(f.body(), pre + "    ", this::ppStmt);
            return;
        }
        if (s instanceof CallStmt c) {
            line("call " + c.name());
            forEach(c.args(), pre, this::ppExpr);
            return;
        }
        if (s instanceof Return r) {
            line("return");
            if (r.value() != null) ppExpr(r.value(), pre, true);
            return;
        }
        if (s instanceof ReturnVoid) { line("return"); return; }
        if (s instanceof Break)       { line("break");  return; }
        if (s instanceof Continue)    { line("continue"); return; }

        line(s.getClass().getSimpleName());
    }


    private void ppExpr(Expr e, String pre, boolean last) {
        if (e instanceof Var v)       { line("variable: " + v.name()); return; }
        if (e instanceof IntLit i)    { line("const " + i.value()); return; }
        if (e instanceof RealLit r)   { line("const " + r.value()); return; }
        if (e instanceof StringLit s) { line("const \"" + s.value() + "\""); return; }
        if (e instanceof BoolLit b)   { line("const " + (b.value() ? "TRUE" : "FALSE")); return; }

        if (e instanceof Unary u) {
            line("un op: " + unOpSymbol(u.op()));
            ppExpr(u.expr(), pre, true);
            return;
        }
        if (e instanceof Binary b) {
            line("bin op: " + binOpSymbol(b.op()));
            out.append(pre).append("├── lhs\n");
            ppExpr(b.left(), pre + "│   ", true);
            out.append(pre).append("└── rhs\n");
            ppExpr(b.right(), pre + "    ", true);
            return;
        }
        if (e instanceof CallExpr c) {
            line("call " + c.name());
            forEach(c.args(), pre, this::ppExpr);
            return;
        }
        if (e instanceof ArrayAccess a) {
            line("index");
            out.append(pre).append("├── array\n");
            ppExpr(a.base(), pre + "│   ", true);
            List<Expr> idx = a.indices();
            if (idx != null && !idx.isEmpty()) {
                out.append(pre).append("└── indexes\n");
                forEach(idx, pre + "    ", this::ppExpr);
            }
            return;
        }
        if (e instanceof Paren p) {
            line("paren");
            ppExpr(p.inner(), pre, true);
            return;
        }

        line(e.getClass().getSimpleName());
    }


    private String unOpSymbol(UnOp op) {
        return switch (op) {
            case POS -> "+";
            case NEG -> "-";
            case NOT -> "NOT";
        };
    }

    private String binOpSymbol(BinOp op) {
        return switch (op) {
            case ADD -> "+"; case SUB -> "-";
            case MUL -> "*"; case DIV -> "/"; case MOD -> "MOD";
            case EQ  -> "="; case NE  -> "#";
            case LT  -> "<"; case LE  -> "<=";
            case GT  -> ">"; case GE  -> ">=";
            case AND -> "AND"; case OR -> "OR";
        };
    }
}
