package app.sem;

import app.ast.*;

import java.util.ArrayList;
import java.util.List;

import static app.sem.TypeUtil.*;

public final class TypeChecker {

    private final Env env = new Env();
    private final ErrorReporter er;
    private final SourceMap smap;

    private boolean insideLoop = false;
    private FuncSym currentFunc = null;

    public TypeChecker(ErrorReporter er, SourceMap smap) {
        this.er = er;
        this.smap = smap;
    }

    public void check(Program prog) {
        env.push();
        installBuiltins();
        for (Decl d : prog.decls()) {
            declareTop(d);
        }
        for (Decl d : prog.decls()) {
            if (d instanceof ProcDecl p) {
                checkProc(p);
            }
            if (d instanceof FuncDecl f) {
                checkFunc(f);
            }
        }

        checkBlock(prog.body());
        env.pop();
    }

    private void installBuiltins() {
//        env.declare(new ProcSym("writeln", List.of()));
//        List<TypeRef> Ts = List.of(Type.INTEGER, Type.REAL, Type.BOOLEAN, Type.STRING);
//        for (TypeRef t : Ts) {
//            env.declare(new ProcSym("writeln", List.of(t)));
//            env.declare(new ProcSym("write", List.of(t)));
//            env.declare(new ProcSym("read", List.of(t)));
//        }
    }

    private void declareTop(Decl d) {
        if (d instanceof VarDecl v) {
            declareVar(v.name(), v.type(), d);
        } else if (d instanceof ProcDecl p) {
            var paramTypes = p.params().stream().map(Param::type).map(TypeRef.class::cast).toList();
            declareProc(p.name(), paramTypes, p);
        } else if (d instanceof FuncDecl f) {
            var paramTypes = f.params().stream().map(Param::type).map(TypeRef.class::cast).toList();
            declareFunc(f.name(), paramTypes, f.retType(), f);
        }
    }

    private void declareVar(String name, TypeRef type, Object where) {
        var s = new VarSym(name, type, false);
        if (!env.declare(s)) {
            er.error(smap.get(where), "redefinition of '%s'", name);
        }
    }

    private void declareProc(String name, List<TypeRef> pts, Object where) {
        var s = new ProcSym(name, pts);
        if (!env.declare(s)) {
            er.error(smap.get(where), "redefinition of '%s'", name);
        }
    }

    private void declareFunc(String name, List<TypeRef> pts, TypeRef ret, Object where) {
        var s = new FuncSym(name, pts, ret);
        if (!env.declare(s)) {
            er.error(smap.get(where), "redefinition of '%s'", name);
        }
    }

    private void checkProc(ProcDecl p) {
        env.push();

        for (Param pa : p.params()) {
            var s = new VarSym(pa.name(), pa.type(), true);
            if (!env.declare(s)) {
                er.error(smap.get(pa), "parameter '%s' shadows existing name", pa.name());
            }
        }

        for (Decl d : p.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type(), v);
            }
        }

        for (Decl d : p.nested()) {
            if (d instanceof ProcDecl q) {
                declareProc(q.name(),
                        q.params().stream().map(Param::type).map(TypeRef.class::cast).toList(), q);
            } else if (d instanceof FuncDecl g) {
                declareFunc(g.name(),
                        g.params().stream().map(Param::type).map(TypeRef.class::cast).toList(), g.retType(), g);
            }
        }

        for (Decl d : p.nested()) {
            if (d instanceof ProcDecl q) {
                checkProc(q);
            } else if (d instanceof FuncDecl g) {
                checkFunc(g);
            }
        }

        var saveLoop = insideLoop;
        insideLoop = false;
        checkBlock(p.body());
        insideLoop = saveLoop;

        env.pop();
    }

    private void checkFunc(FuncDecl f) {
        env.push();
        var oldFunc = currentFunc;
        currentFunc = new FuncSym(f.name(),
                f.params().stream().map(Param::type).map(TypeRef.class::cast).toList(),
                f.retType());

        for (Param pa : f.params()) {
            var s = new VarSym(pa.name(), pa.type(), true);
            if (!env.declare(s)) {
                er.error(smap.get(pa), "parameter '%s' shadows existing name", pa.name());
            }
        }

        for (Decl d : f.locals()) {
            if (d instanceof VarDecl v) {
                declareVar(v.name(), v.type(), v);
            }
        }

        for (Decl d : f.nested()) {
            if (d instanceof ProcDecl q) {
                declareProc(q.name(),
                        q.params().stream().map(Param::type).map(TypeRef.class::cast).toList(), q);
            } else if (d instanceof FuncDecl g) {
                declareFunc(g.name(),
                        g.params().stream().map(Param::type).map(TypeRef.class::cast).toList(), g.retType(), g);
            }
        }
        for (Decl d : f.nested()) {
            if (d instanceof ProcDecl q) {
                checkProc(q);
            } else if (d instanceof FuncDecl g) {
                checkFunc(g);
            }
        }

        var saveLoop = insideLoop;
        insideLoop = false;
        checkBlock(f.body());
        insideLoop = saveLoop;

        currentFunc = oldFunc;
        env.pop();
    }

    private void checkBlock(List<Stmt> stmts) {
        for (Stmt s : stmts) {
            checkStmt(s);
        }
    }

    private void checkStmt(Stmt s) {
        if (s instanceof Assign a) {
            TypeRef lhs = typeOfLvalue(a.name(), s);
            TypeRef rhs = typeOf(a.value());
            if (lhs == null) {
                return;
            }
            if (rhs == null) {
                return;
            }
            if (!same(lhs, rhs)) {
                er.error(smap.get(s), "type mismatch in assignment: '%s' := '%s'",
                        show(lhs), show(rhs));
            }
        } else if (s instanceof AssignIndex ai) {
            TypeRef t = typeOf(ai.target());
            if (t instanceof ArrayType) {
                er.error(smap.get(s), "indexed assignment must target an array element, got %s", show(t));
                return;
            }
            TypeRef rhs = typeOf(ai.value());
            if (t == null || rhs == null) {
                return;
            }
            if (!same(t, rhs)) {
                er.error(smap.get(s), "type mismatch in indexed assignment: '%s' := '%s'", show(t), show(rhs));
            }
        } else if (s instanceof CallStmt c) {
            if (c.name().equals("writeln")) {
                if (c.args().size() > 1) {
                    er.error(smap.get(c), "writeln expects 0 or 1 argument");
                    return;
                }
                if (c.args().size() == 1) {
                    var t = typeOf(c.args().get(0));
                    if (!(isInteger(t) || isReal(t) || isBoolean(t) || isString(t))) {
                        er.error(smap.get(c.args().get(0)), "writeln: unsupported type %s", show(t));
                    }
                }
                return;
            }
            if (c.name().equals("write")) {
                if (c.args().size() != 1) {
                    er.error(smap.get(c), "write expects 1 argument");
                    return;
                }
                var t = typeOf(c.args().get(0));
                if (!(isInteger(t) || isReal(t) || isBoolean(t) || isString(t))) {
                    er.error(smap.get(c.args().get(0)), "write: unsupported type %s", show(t));
                }
                return;
            }
            if (c.name().equals("read")) {
                if (c.args().size() != 1) {
                    er.error(smap.get(c), "read expects 1 argument (lvalue)");
                    return;
                }
                Expr a = c.args().get(0);
                TypeRef t;
                if (a instanceof Var v) {
                    t = typeOf(v);
                } else if (a instanceof ArrayAccess aa) {
                    t = typeOf(aa);
                    if (t instanceof ArrayType) {
                        er.error(smap.get(a), "read target must be an array element, got %s", show(t));
                        return;
                    }
                } else {
                    er.error(smap.get(a), "read argument must be a variable or array element");
                    return;
                }
                if (!(isInteger(t) || isReal(t) || isBoolean(t) || isString(t))) {
                    er.error(smap.get(a), "read: unsupported type %s", show(t));
                }
                return;
            }

            var sym = env.lookup(c.name());
            if (sym == null) {
                er.error(smap.get(c), "unknown procedure/function '%s'", c.name());
                return;
            }
            if (sym instanceof FuncSym) {
                er.error(smap.get(c), "cannot use function '%s' as a statement (result ignored)", c.name());
                return;
            }
            if (!(sym instanceof ProcSym p)) {
                er.error(smap.get(c), "'%s' is not a procedure", c.name());
                return;
            }
            checkCallArgs(p.paramTypes(), c.args(), c);
        } else if (s instanceof If i) {
            TypeRef c = typeOf(i.cond());
            if (!isBoolean(c)) {
                er.error(smap.get(i), "if condition must be boolean, got %s", show(c));
            }
            checkBlock(i.thenPart());
            for (ElseIf ei : i.elseIfs()) {
                TypeRef cc = typeOf(ei.cond());
                if (!isBoolean(cc)) {
                    er.error(smap.get(ei), "elseif condition must be boolean, got %s", show(cc));
                }
                checkBlock(ei.body());
            }
            checkBlock(i.elsePart());
        } else if (s instanceof While w) {
            TypeRef c = typeOf(w.cond());
            if (!isBoolean(c)) {
                er.error(smap.get(w), "while condition must be boolean, got %s", show(c));
            }
            var save = insideLoop;
            insideLoop = true;
            checkBlock(w.body());
            insideLoop = save;
        } else if (s instanceof Repeat r) {
            var save = insideLoop;
            insideLoop = true;
            checkBlock(r.body());
            insideLoop = save;
            TypeRef c = typeOf(r.cond());
            if (!isBoolean(c)) {
                er.error(smap.get(r), "repeat-until condition must be boolean, got %s", show(c));
            }
        } else if (s instanceof For f) {
            TypeRef idx = typeOfLvalue(f.var(), s);
            if (idx == null) {
                return;
            }
            if (!isInteger(idx)) {
                er.error(smap.get(s), "for index '%s' must be integer", f.var());
            }
            TypeRef lo = typeOf(f.from());
            TypeRef hi = typeOf(f.to());
            if (!isInteger(lo) || !isInteger(hi)) {
                er.error(smap.get(s), "for bounds must be integer, got %s and %s", show(lo), show(hi));
            }
            var save = insideLoop;
            insideLoop = true;
            checkBlock(f.body());
            insideLoop = save;
        } else if (s instanceof Break || s instanceof Continue) {
            if (!insideLoop) {
                er.error(smap.get(s), "%s used outside of loop",
                        (s instanceof Break) ? "break" : "continue");
            }
        } else if (s instanceof Return r) {
            if (currentFunc == null) {
                // мы внутри процедуры
                if (r.value() != null) {
                    er.error(smap.get(r), "return with a value in a procedure");
                }
            } else {
                if (r.value() == null) {
                    er.error(smap.get(r), "missing return value (function returns %s)",
                            show(currentFunc.returnType()));
                } else {
                    TypeRef got = typeOf(r.value());
                    if (!same(got, currentFunc.returnType())) {
                        er.error(smap.get(r), "return type mismatch: expected %s, got %s",
                                show(currentFunc.returnType()), show(got));
                    }
                }
            }
        }
    }

    private void checkCallArgs(List<TypeRef> paramTypes, List<Expr> args, Object where) {
        if (paramTypes.size() != args.size()) {
            er.error(smap.get(where), "invalid argument count: expected %d, got %d",
                    paramTypes.size(), args.size());
            return;
        }
        for (int i = 0; i < paramTypes.size(); i++) {
            TypeRef expected = paramTypes.get(i);
            TypeRef actual = typeOf(args.get(i));
            if (!same(expected, actual)) {
                er.error(smap.get(args.get(i)), "argument #%d: expected %s, got %s",
                        i + 1, show(expected), show(actual));
            }
        }
    }

    private TypeRef typeOf(Expr e) {
        if (e instanceof IntLit) {
            return Type.INTEGER;
        }
        if (e instanceof RealLit) {
            return Type.REAL;
        }
        if (e instanceof StringLit) {
            return Type.STRING;
        }
        if (e instanceof BoolLit) {
            return Type.BOOLEAN;
        }
        if (e instanceof Var v) {
            var s = env.lookup(v.name());
            if (s == null) {
                er.error(smap.get(e), "undeclared identifier '%s'", v.name());
                return Type.INTEGER;
            }
            if (s instanceof VarSym vs) {
                return vs.type();
            }
            er.error(smap.get(e), "'%s' is not a variable", v.name());
            return Type.INTEGER;
        }
        if (e instanceof ArrayAccess a) {
            TypeRef base = typeOf(a.base());
            if (!(base instanceof ArrayType at)) {
                er.error(smap.get(e), "indexing non-array value of type %s", show(base));
                return Type.INTEGER;
            }
            for (Expr idx : a.indices()) {
                TypeRef ti = typeOf(idx);
                if (!isInteger(ti)) {
                    er.error(smap.get(idx), "array index must be integer, got %s", show(ti));
                }
            }
            int used = a.indices().size();
            if (used > at.dimensions().size()) {
                er.error(smap.get(e), "too many indices (has %d dims, used %d)", at.dimensions().size(), used);
                return at.elementType();
            }
            if (used == at.dimensions().size()) {
                return at.elementType();
            }
            return new ArrayType(at.elementType(), at.dimensions().subList(used, at.dimensions().size()));
        }
        if (e instanceof Paren p) {
            return typeOf(p.inner());
        }
        if (e instanceof Unary u) {
            TypeRef t = typeOf(u.value());
            TypeRef r = resultOfUnary(u.op(), t);
            if (r == null) {
                er.error(smap.get(e), "invalid unary '%s' for type %s", u.op(), show(t));
            }
            return r == null ? Type.INTEGER : r;
        }
        if (e instanceof Binary b) {
            TypeRef L = typeOf(b.left());
            TypeRef R = typeOf(b.right());
            TypeRef r = resultOfBinary(b.op(), L, R);
            if (r == null) {
                er.error(smap.get(e), "invalid binary '%s' for %s and %s", b.op(), show(L), show(R));
            }
            return r == null ? Type.INTEGER : r;
        }
        if (e instanceof CallExpr c) {
            var s = env.lookup(c.name());
            if (s == null) {
                er.error(smap.get(e), "unknown function '%s'", c.name());
                return Type.INTEGER;
            }
            if (!(s instanceof FuncSym f)) {
                er.error(smap.get(e), "'%s' is not a function", c.name());
                return Type.INTEGER;
            }
            checkCallArgs(f.paramTypes(), c.args(), e);
            return f.returnType();
        }
        er.error(smap.get(e), "internal: unknown expr node %s", e.getClass().getSimpleName());
        return Type.INTEGER;
    }

    private TypeRef typeOfLvalue(String name, Object where) {
        var s = env.lookup(name);
        if (s == null) {
            er.error(smap.get(where), "undeclared identifier '%s'", name);
            return null;
        }
        if (s instanceof VarSym vs) {
            return vs.type();
        }
        er.error(smap.get(where), "'%s' is not a variable", name);
        return null;
    }
}
