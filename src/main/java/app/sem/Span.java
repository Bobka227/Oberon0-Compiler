package app.sem;

public record Span(String file, int line, int col) {
    @Override public String toString() { return file + ":" + line + ":" + col; }
}
