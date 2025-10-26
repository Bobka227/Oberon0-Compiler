package app.sem;

import app.ast.*;

import java.util.List;
import java.util.Objects;

public final class TypeUtil {
    private TypeUtil(){}

    public static boolean same(TypeRef a, TypeRef b) {
        if (a instanceof Type ta && b instanceof Type tb) return ta == tb;
        if (a instanceof ArrayType aa && b instanceof ArrayType ab) {
            return same(aa.elementType(), ab.elementType()) && aa.dimensions().equals(ab.dimensions());
        }
        return false;
    }

    public static String show(TypeRef t) {
        if (t instanceof Type bt) return bt.name().toLowerCase();
        if (t instanceof ArrayType at) {
            return "array[" + String.join(",", at.dimensions().stream().map(Object::toString).toList())
                    + "] of " + show(at.elementType());
        }
        return String.valueOf(t);
    }

    public static boolean isScalar(TypeRef t) { return t instanceof Type; }
    public static boolean isInteger(TypeRef t){ return t == Type.INTEGER; }
    public static boolean isBoolean(TypeRef t){ return t == Type.BOOLEAN; }
    public static boolean isReal(TypeRef t)   { return t == Type.REAL; }
    public static boolean isString(TypeRef t) { return t == Type.STRING; }

    public static TypeRef resultOfUnary(UnOp op, TypeRef arg) {
        return switch (op) {
            case POS, NEG -> (isInteger(arg) || isReal(arg)) ? arg : null;
            case NOT      -> isBoolean(arg) ? Type.BOOLEAN : null;
        };
    }

    public static TypeRef resultOfBinary(BinOp op, TypeRef L, TypeRef R) {
        switch (op) {
            case ADD, SUB, MUL, DIV:
                if (same(L, R) && (isInteger(L) || isReal(L))) return L;
                return null;
            case MOD:
                if (same(L, R) && isInteger(L)) return Type.INTEGER;
                return null;
            case AND, OR:
                if (same(L, R) && isBoolean(L)) return Type.BOOLEAN;
                return null;
            case EQ, NE, LT, LE, GT, GE: // сравнения
                if (same(L, R) && (isInteger(L) || isReal(L) || isBoolean(L) || isString(L))) return Type.BOOLEAN;
                return null;
        }
        return null;
    }
}
