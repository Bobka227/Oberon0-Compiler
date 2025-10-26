package app.sem;

import java.util.*;

public final class Env {
    private final Deque<Map<String, Symbol>> stack = new ArrayDeque<>();

    public Env() { push(); } 

    public void push() { stack.push(new HashMap<>()); }
    public void pop()  { 
        if (stack.isEmpty()) throw new IllegalStateException("Env.pop(): empty stack");
        stack.pop(); 
    }

    public boolean declare(Symbol s) {
        if (stack.isEmpty()) throw new IllegalStateException("Env.declare(): no scope");
        return stack.peek().putIfAbsent(s.name(), s) == null;
    }

    public Symbol lookup(String name) {
        for (var scope : stack) {
            var s = scope.get(name);
            if (s != null) return s;
        }
        return null;
    }

    public boolean isDeclaredHere(String name) {
        if (stack.isEmpty()) throw new IllegalStateException("Env.isDeclaredHere(): no scope");
        return stack.peek().containsKey(name);
    }
}
