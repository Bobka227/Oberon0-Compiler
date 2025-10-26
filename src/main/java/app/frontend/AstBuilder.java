package app.frontend;

import app.ast.*;
import app.parser.Oberon0BaseVisitor;
import app.parser.Oberon0Parser;
import app.sem.SourceMap;
import app.sem.Span;

import java.util.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class AstBuilder extends Oberon0BaseVisitor<Object> {

    private final SourceMap smap;

    public AstBuilder(SourceMap smap) {
        this.smap = smap;
    }

    private <T> T mark(TerminalNode tn, T node) {
        smap.put(node, tn.getSymbol());
        return node;
    }

    private <T> T mark(ParserRuleContext ctx, T node) {
        smap.put(node, ctx);
        return node;
    }

    @SuppressWarnings("unchecked")
    public Program build(Oberon0Parser.ModuleContext ctx) {
        String startName = ctx.ID(0).getText();
        String endName = ctx.ID(1).getText();
        if (!startName.equals(endName)) {
            var tok = ctx.ID(1).getSymbol();
            throw new IllegalStateException(String.format(
                    "Module name after END must match MODULE name (got '%s', expected '%s') at %d:%d",
                    endName, startName, tok.getLine(), tok.getCharPositionInLine() + 1
            ));
        }

        String name = startName;

        List<Decl> decls = new ArrayList<>();
        List<Stmt> body = new ArrayList<>();

        var declsCtx = ctx.declarations();
        if (declsCtx != null) {
            decls.addAll((List<Decl>) visit(declsCtx));
        }
        var stmtsCtx = ctx.statements();
        if (stmtsCtx != null) {
            body.addAll((List<Stmt>) visit(stmtsCtx));
        }

        Program p = new Program(name, decls, body);
        smap.put(p, ctx);
        return p;
    }

    @Override
    public Object visitDeclarations(Oberon0Parser.DeclarationsContext ctx) {
        List<Decl> all = new ArrayList<>();

        for (var vd : ctx.vardecl()) {
            @SuppressWarnings("unchecked")
            var decls = (List<Decl>) visit(vd);
            all.addAll(decls);
        }

        if (ctx.procdecl_list() != null) {
            @SuppressWarnings("unchecked")
            var procs = (List<Decl>) visit(ctx.procdecl_list());
            all.addAll(procs);
        }
        return all;
    }

    @Override
    public Object visitVardecl(Oberon0Parser.VardeclContext ctx) {
        List<Decl> list = new ArrayList<>();
        var vlist = ctx.vardecl_list();
        int groups = vlist.vartype().size();
        for (int i = 0; i < groups; i++) {
            var ids = vlist.idlist(i).ID();
            var tref = mapTypeRef(vlist.vartype(i));
            for (var idTok : ids) {
                list.add(mark(idTok, new VarDecl(idTok.getText(), tref)));
            }
        }
        return list;
    }

    private TypeRef mapTypeRef(Oberon0Parser.VartypeContext ctx) {
        if (ctx.basetype() != null) {
            String t = ctx.basetype().getText().toLowerCase(Locale.ROOT);
            return switch (t) {
                case "boolean" ->
                    Type.BOOLEAN;
                case "integer" ->
                    Type.INTEGER;
                case "real" ->
                    Type.REAL;
                case "string" ->
                    Type.STRING;
                default ->
                    throw new IllegalArgumentException("Unknown basetype: " + t);
            };
        }
        var a = ctx.arraytype();
        List<Integer> dims = new ArrayList<>();
        for (var n : a.dim_list().INTEGER_LITERAL()) {
            dims.add(Integer.parseInt(n.getText()));
        }
        TypeRef elem = mapTypeRef(a.vartype());
        return new ArrayType(elem, dims);
    }

    @Override
    public Object visitProcdecl_list(Oberon0Parser.Procdecl_listContext ctx) {
        List<Decl> list = new ArrayList<>();
        for (var p : ctx.procdecl()) {
            list.add((Decl) visit(p));
        }
        return list;
    }

    @Override
    public Object visitProcdecl(Oberon0Parser.ProcdeclContext ctx) {
        var header = ctx.procheader();
        var body = ctx.procbody();

        @SuppressWarnings("unchecked")
        List<Param> params = header.formalpars() != null
                ? (List<Param>) visit(header.formalpars())
                : List.of();

        List<Decl> locals = body.vardecl() != null ? (List<Decl>) visit(body.vardecl()) : List.of();
        List<Decl> nested = body.procdecl_list() != null ? (List<Decl>) visit(body.procdecl_list()) : List.of();
        List<Stmt> stmts = body.statements() != null ? (List<Stmt>) visit(body.statements()) : List.of();

        String name = header.ID().getText();
        if (header.PROCEDURE() != null) {
            return mark(ctx, new ProcDecl(name, params, locals, nested, stmts));
        } else {
            Type ret = mapType(header.vartype());
            return mark(ctx, new FuncDecl(name, params, ret, locals, nested, stmts));
        }
    }

    @Override
    public Object visitFormalpars(Oberon0Parser.FormalparsContext ctx) {
        if (ctx.fpsection_list() == null) {
            return List.<Param>of();
        }
        return visit(ctx.fpsection_list());
    }

    @Override
    public Object visitFpsection_list(Oberon0Parser.Fpsection_listContext ctx) {
        List<Param> ps = new ArrayList<>();
        for (var s : ctx.fpsection()) {
            ps.addAll((List<Param>) visit(s));
        }
        return ps;
    }

    @Override
    public Object visitFpsection(Oberon0Parser.FpsectionContext ctx) {
        List<Param> ps = new ArrayList<>();
        TypeRef t = mapTypeRef(ctx.vartype());   
        for (var idTok : ctx.idlist().ID()) {
            ps.add(mark(idTok, new Param(idTok.getText(), t)));
        }
        return ps;
    }

    private Type mapType(Oberon0Parser.VartypeContext ctx) {
        if (ctx.basetype() == null) {

            throw new IllegalArgumentException("Expected basetype, got: " + ctx.getText());
        }
        String t = ctx.basetype().getText().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "boolean" ->
                Type.BOOLEAN;
            case "integer" ->
                Type.INTEGER;
            case "real" ->
                Type.REAL;
            case "string" ->
                Type.STRING;
            default ->
                throw new IllegalArgumentException("Unknown basetype: " + t);
        };
    }

    @Override
    public Object visitStatements(Oberon0Parser.StatementsContext ctx) {
        List<Stmt> list = new ArrayList<>();
        for (var s : ctx.statement()) {
            list.add((Stmt) visit(s));
        }
        return list;
    }

    @Override
    public Object visitAssignment(Oberon0Parser.AssignmentContext ctx) {
        Expr lhs = (Expr) visit(ctx.variable());
        Expr rhs = (Expr) visit(ctx.expression());
        if (lhs instanceof ArrayAccess acc) {
            return mark(ctx, new AssignIndex(acc, rhs));
        }
        if (lhs instanceof Var v) {
            return mark(ctx, new Assign(v.name(), rhs));
        }
        throw new IllegalStateException("Invalid LHS");
    }

    @Override
    public Object visitProccall(Oberon0Parser.ProccallContext ctx) {
        String name = ctx.ID().getText();
        List<Expr> args = List.of();
        if (ctx.actualpar() != null && ctx.actualpar().expression_list() != null) {
            @SuppressWarnings("unchecked")
            List<Expr> xs = (List<Expr>) visit(ctx.actualpar().expression_list());
            args = xs;
        }
        return mark(ctx, new CallStmt(name, args));
    }

    @Override
    public Object visitExpression_list(Oberon0Parser.Expression_listContext ctx) {
        List<Expr> list = new ArrayList<>();
        for (var e : ctx.expression()) {
            list.add((Expr) visit(e));
        }
        return list;
    }

    @Override
    public Object visitConditional(Oberon0Parser.ConditionalContext ctx) {
        Expr cond = (Expr) visit(ctx.expression(0));
        List<Stmt> thenPart = ctx.statements(0) != null ? (List<Stmt>) visit(ctx.statements(0)) : List.of();

        List<ElseIf> elsifs = new ArrayList<>();
        int k = ctx.ELSEIF().size();
        for (int i = 0; i < k; i++) {
            Expr c = (Expr) visit(ctx.expression(i + 1));
            List<Stmt> b = ctx.statements(i + 1) != null ? (List<Stmt>) visit(ctx.statements(i + 1)) : List.of();
            elsifs.add(new ElseIf(c, b));
        }
        List<Stmt> elsePart = List.of();
        if (ctx.ELSE() != null) {
            var lastStmts = ctx.statements(k + 1);
            elsePart = lastStmts != null ? (List<Stmt>) visit(lastStmts) : List.of();
        }
        return mark(ctx, new If(cond, thenPart, elsifs, elsePart));
    }

    @Override
    public Object visitRepetition(Oberon0Parser.RepetitionContext ctx) {
        if (ctx.WHILE() != null) {
            @SuppressWarnings("unchecked")
            List<Stmt> body = ctx.statements() != null ? (List<Stmt>) visit(ctx.statements()) : List.of();
            return mark(ctx, new While((Expr) visit(ctx.expression(0)), body));
        } else if (ctx.REPEAT() != null) {
            @SuppressWarnings("unchecked")
            List<Stmt> body = ctx.statements() != null ? (List<Stmt>) visit(ctx.statements()) : List.of();
            return mark(ctx, new Repeat(body, (Expr) visit(ctx.expression(0))));
        } else {
            @SuppressWarnings("unchecked")
            List<Stmt> body = ctx.statements() != null ? (List<Stmt>) visit(ctx.statements()) : List.of();
            return mark(ctx, new For(ctx.ID().getText(),
                    (Expr) visit(ctx.expression(0)),
                    (Expr) visit(ctx.expression(1)),
                    body));
        }
    }

    @Override
    public Object visitIo_statement(Oberon0Parser.Io_statementContext ctx) {
        String name = ctx.WRITE() != null ? "write" : (ctx.WRITELN() != null ? "writeln" : "read");
        List<Expr> args = new ArrayList<>();
        if (ctx.expression_list() != null) {
            @SuppressWarnings("unchecked")
            List<Expr> xs = (List<Expr>) visit(ctx.expression_list());
            args = xs;
        }
        return mark(ctx, new CallStmt(name, args));
    }

    @Override
    public Object visitStatement(Oberon0Parser.StatementContext ctx) {
        if (ctx.RETURN() != null) {
            if (ctx.expression() != null) {
                return mark(ctx, new Return((Expr) visit(ctx.expression())));
            }
            return mark(ctx, new ReturnVoid());
        }
        if (ctx.BREAK() != null) {
            return mark(ctx, new Break());
        }
        if (ctx.CONTINUE() != null) {
            return mark(ctx, new Continue());
        }
        return super.visitStatement(ctx);
    }

    @Override
    public Object visitLiteral(Oberon0Parser.LiteralContext ctx) {
        if (ctx.BOOLEAN_LITERAL() != null) {
            return mark(ctx, new BoolLit(ctx.BOOLEAN_LITERAL().getText().equals("TRUE")));
        }
        if (ctx.INTEGER_LITERAL() != null) {
            return mark(ctx, new IntLit(Integer.parseInt(ctx.INTEGER_LITERAL().getText())));
        }
        if (ctx.REAL_LITERAL() != null) {
            return mark(ctx, new RealLit(Double.parseDouble(ctx.REAL_LITERAL().getText())));
        }
        String raw = ctx.STRING_LITERAL().getText();
        return mark(ctx, new StringLit(raw.substring(1, raw.length() - 1)));
    }

    @Override
    public Object visitPrimary(Oberon0Parser.PrimaryContext ctx) {
        if (ctx.variable() != null) {
            return visit(ctx.variable());
        }
        if (ctx.proccall() != null) {
            var call = (CallStmt) visit(ctx.proccall());
            return mark(ctx, new CallExpr(call.name(), call.args()));  // <-- mark
        }
        if (ctx.expression() != null) {
            return mark(ctx, new Paren((Expr) visit(ctx.expression()))); // <-- mark
        }
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        return super.visitPrimary(ctx);
    }

    @Override
    public Object visitUnary(Oberon0Parser.UnaryContext ctx) {
        if (ctx.PLUS() != null || ctx.MINUS() != null || ctx.NOT() != null) {
            String opTok = ctx.getChild(0).getText();
            UnOp op = switch (opTok) {
                case "+" ->
                    UnOp.POS;
                case "-" ->
                    UnOp.NEG;
                default ->
                    UnOp.NOT;
            };
            return mark(ctx, new Unary(op, (Expr) visit(ctx.primary())));   // <-- mark
        }
        return visit(ctx.primary());
    }

    @Override
    public Object visitMultiplicative(Oberon0Parser.MultiplicativeContext ctx) {
        Expr e = (Expr) visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            BinOp bop = switch (op) {
                case "*" ->
                    BinOp.MUL;
                case "/" ->
                    BinOp.DIV;
                default ->
                    BinOp.MOD;
            };
            e = mark(ctx, new Binary(bop, e, (Expr) visit(ctx.unary(i))));  // <-- mark на каждое построение
        }
        return e;
    }

    @Override
    public Object visitAdditive(Oberon0Parser.AdditiveContext ctx) {
        Expr e = (Expr) visit(ctx.multiplicative(0));
        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            e = mark(ctx, new Binary(op.equals("+") ? BinOp.ADD : BinOp.SUB, e, (Expr) visit(ctx.multiplicative(i))));
        }
        return e;
    }

    @Override
    public Object visitRelation(Oberon0Parser.RelationContext ctx) {
        Expr left = (Expr) visit(ctx.additive(0));
        if (ctx.relop() == null) {
            return left;
        }
        Expr right = (Expr) visit(ctx.additive(1));
        String op = ctx.relop().getText();
        BinOp bop = switch (op) {
            case "=" ->
                BinOp.EQ;
            case "#" ->
                BinOp.NE;
            case "<" ->
                BinOp.LT;
            case "<=" ->
                BinOp.LE;
            case ">" ->
                BinOp.GT;
            default ->
                BinOp.GE;
        };
        return mark(ctx, new Binary(bop, left, right));
    }

    @Override
    public Object visitLogicAnd(Oberon0Parser.LogicAndContext ctx) {
        Expr e = (Expr) visit(ctx.relation(0));
        for (int i = 1; i < ctx.relation().size(); i++) {
            e = mark(ctx, new Binary(BinOp.AND, e, (Expr) visit(ctx.relation(i))));
        }
        return e;
    }

    @Override
    public Object visitLogicOr(Oberon0Parser.LogicOrContext ctx) {
        Expr e = (Expr) visit(ctx.logicAnd(0));
        for (int i = 1; i < ctx.logicAnd().size(); i++) {
            e = mark(ctx, new Binary(BinOp.OR, e, (Expr) visit(ctx.logicAnd(i))));
        }
        return e;
    }

    @Override
    public Object visitVariable(Oberon0Parser.VariableContext v) {
        Expr cur = new Var(v.ID().getText());

        for (Oberon0Parser.Expression_listContext el : v.expression_list()) {
            @SuppressWarnings("unchecked")
            List<Expr> idx = (List<Expr>) visit(el); // visitExpression_list -> List<Expr>
            cur = new ArrayAccess(cur, idx);
        }

        return mark(v, cur);
    }

    private Expr buildVariable(Oberon0Parser.VariableContext v) {
        Expr cur = new Var(v.ID().getText());
        for (Oberon0Parser.Expression_listContext el : v.expression_list()) {
            @SuppressWarnings("unchecked")
            List<Expr> idx = (List<Expr>) visit(el);
            cur = new ArrayAccess(cur, idx);
        }
        return cur;
    }

}
