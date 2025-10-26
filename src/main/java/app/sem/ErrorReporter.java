package app.sem;

import java.util.ArrayList;
import java.util.List;

public final class ErrorReporter {
    private final List<String> errors = new ArrayList<>();
    private boolean headerPrinted = false;

    public void error(Span s, String fmt, Object... args) {
        String msg = String.format(fmt, args);
        errors.add("%s: error: %s".formatted(s, msg));
    }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public void dump() {
        if (errors.isEmpty()) return;
        if (!headerPrinted) {
            System.err.println("=== SEMANTIC ERRORS ===");
            headerPrinted = true;
        }
        errors.forEach(System.err::println);
    }

    public List<String> all() { return errors; }
}

