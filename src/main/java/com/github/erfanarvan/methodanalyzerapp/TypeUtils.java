package com.github.erfanarvan.methodanalyzerapp;

import java.util.Set;

public class TypeUtils {

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void"
    );

    /**
     * Checks if a given type is a standard Java type.
     * <p>
     * A type is considered standard if it is:
     * <ul>
     *   <li>A primitive type (e.g., int, boolean, double, etc.).</li>
     *   <li>Part of the `java.*` package (e.g., java.lang.String, java.util.List).</li>
     * </ul>
     * </p>
     *
     * @param type The fully qualified or simple type name.
     * @return {@code true} if the type is a standard Java type, otherwise {@code false}.
     */
    public static boolean isStandardType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }

        // check for primitive types
        if (PRIMITIVE_TYPES.contains(type)) {
            return true;
        }

        // check if type starts with "java."
        return type.startsWith("java.");
    }
}
