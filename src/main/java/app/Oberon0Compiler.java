package app;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import app.parser.Oberon0Lexer;
import app.parser.Oberon0Parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*; // NEW

import app.frontend.AstBuilder;
import app.frontend.AstPrinter;
import app.ast.Program;

import app.sem.SourceMap;
import app.sem.ErrorReporter;
import app.sem.TypeChecker;

public class Oberon0Compiler {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: mvn -q exec:java \"-Dexec.args=examples/hello.ob0 [--emit-c out.c] [--no-run]\"");
            System.exit(1);
        }

        String file = args[0];
        if (!file.toLowerCase().endsWith(".ob0")) {
            System.err.println("Error: expected a .ob0 source file, got: " + file);
            System.err.println("Usage: mvn -q exec:java \"-Dexec.args=path/to/file.ob0 [--emit-c out.c] [--no-run]\"");
            System.exit(1);
        }
        String src = Files.readString(Path.of(file));

        // --- lex/parse ---
        Oberon0Lexer lexer = new Oberon0Lexer(CharStreams.fromString(src));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Oberon0Parser parser = new Oberon0Parser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        Oberon0Parser.ModuleContext root;
        try {
            root = parser.module();
        } catch (ParseCancellationException ex) {
            System.err.println(ex.getMessage());
            System.exit(2);
            return;
        }

        SourceMap smap = new SourceMap(file);
        AstBuilder builder = new AstBuilder(smap);
        Program ast;
        try {
            ast = builder.build(root);
        } catch (IllegalStateException ex) {
            System.err.println(ex.getMessage());
            System.exit(3);
            return;
        }

        System.out.println("Parse OK");

        ErrorReporter er = new ErrorReporter();
        TypeChecker tc = new TypeChecker(er, smap);
        tc.check(ast);

        if (er.hasErrors()) {
            System.err.println("=== SEMANTIC ERRORS ===");
            er.dump();
            System.exit(4);
        }

     
        boolean emitC = false;
        boolean run = true;
        String outC = null;

        for (int i = 1; i < args.length; i++) {
            if ("--emit-c".equals(args[i]) && i + 1 < args.length) {
                emitC = true;
                outC = args[++i];
            } else if ("--no-run".equals(args[i])) {
                run = false;
            }
        }

        String cCode = new app.backend.CCodegen(ast.name()).generate(ast);

        if (emitC) {
            if (outC == null) {
                outC = file.substring(0, file.length() - ".ob0".length()) + ".c";
            }
            Files.writeString(Path.of(outC), cCode);
            System.out.println("C code generated -> " + outC);
            if (!run) {
            
                return;
            }
        }

        Path tmpDir = Files.createTempDirectory("ob0_run_");
        try {
            Path cFile = tmpDir.resolve(ast.name() + ".c");
            Files.writeString(cFile, cCode);

            String cc = System.getenv().getOrDefault("CC", "gcc");
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            Path exePath = tmpDir.resolve(isWindows ? ast.name() + ".exe" : ast.name());

            List<String> cmd = new ArrayList<>();
            cmd.add(cc);
            cmd.add("-std=c11");
            cmd.add(cFile.toString());
            cmd.add("-o");
            cmd.add(exePath.toString());
            cmd.add("-lm");

            ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
            Process compile = pb.start();
            int ccExit = compile.waitFor();
            if (ccExit != 0) {
                System.err.println("C compilation failed (exit " + ccExit + ")");
                System.err.println("Temp kept at: " + tmpDir);
                System.exit(5);
            }

            if (run) {
                ProcessBuilder runPb = new ProcessBuilder(exePath.toString()).inheritIO();
                Process prog = runPb.start();
                int rc = prog.waitFor();
                System.exit(rc);
            } else {
                System.out.println("Built temporary executable at: " + exePath);
            }
        } finally {
            try {
                Files.walk(tmpDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignore) {
                            }
                        });
            } catch (Exception ignore) {
            }
        }

    }

    static class ThrowingErrorListener extends BaseErrorListener {

        static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col,
                String msg, RecognitionException e) {
            throw new ParseCancellationException("Syntax error at " + line + ":" + col + " - " + msg);
        }
    }
}
