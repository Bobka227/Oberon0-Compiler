//package app.backend;
//
//import app.parser.*;
//import app.frontend.AstBuilder;
//import app.ast.Program;
//import app.sem.SourceMap;
//import app.sem.ErrorReporter;
//import app.sem.TypeChecker;
//
//import org.antlr.v4.runtime.*;
//
//import java.nio.file.*;
//
//public final class CompileTool {
//    public static void main(String[] args) throws Exception {
//        if (args.length < 2) {
//            System.err.println("Usage: java app.backend.CompileTool <input.ob0> <output.c>");
//            System.exit(2);
//        }
//
//        String inPath  = args[0];
//        String outPath = args[1];
//        String src     = Files.readString(Path.of(inPath));
//
//        // 1) Парсер ANTLR
//        CharStream input = CharStreams.fromString(src);
//        Oberon0Lexer  lex = new Oberon0Lexer(input);
//        CommonTokenStream toks = new CommonTokenStream(lex);
//        Oberon0Parser par = new Oberon0Parser(toks);
//
//        par.removeErrorListeners();
//        par.addErrorListener(new BaseErrorListener() {
//            @Override public void syntaxError(Recognizer<?,?> r, Object o, int line, int col, String msg, RecognitionException e) {
//                throw new RuntimeException("Syntax error at "+line+":"+(col+1)+" "+msg);
//            }
//        });
//
//        Oberon0Parser.ModuleContext tree = par.module();
//
//        SourceMap smap = new SourceMap(inPath);
//        Program ast = new AstBuilder(smap).build(tree);
//
//        ErrorReporter er = new ErrorReporter();
//        TypeChecker tc = new TypeChecker( er, smap);
//        tc.check(ast);
//
//        if (er.hasErrors()) {
//            er.dump();
//            System.exit(3);
//        }
//
//        String c = new CCodegen(ast.name()).generate(ast);
//        Files.writeString(Path.of(outPath), c);
//        System.out.println("OK → " + outPath);
//    }
//}
