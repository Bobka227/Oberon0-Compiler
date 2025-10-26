// app/ast/ArrayType.java
package app.ast;

import java.util.List;

public record ArrayType(TypeRef elementType, List<Integer> dimensions) implements TypeRef { }
