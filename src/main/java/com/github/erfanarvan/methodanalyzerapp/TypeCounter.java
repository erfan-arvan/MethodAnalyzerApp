package com.github.erfanarvan.methodanalyzerapp;

import java.util.*;

public class TypeCounter {

    // list of primitive types in Java
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "int", "boolean", "double", "char", "byte", "short", "long", "float", "void"
    );

    public static Map<String, List<String>> countStandardAndCustomTypes(List<String> expressionTypes) {
        // sets for unique standard & custom types
        Set<String> standardTypesSet = new HashSet<>();
        Set<String> customTypesSet = new HashSet<>();

        // list to store unresolved types (with duplicates counted)
        List<String> unresolvedList = new ArrayList<>();

        for (String type : expressionTypes) {
            if (type.startsWith("java.") || PRIMITIVE_TYPES.contains(type)) {
                standardTypesSet.add(type); // track unique standard Java types
            } else if (type.equals("Unresolved") || type.equals("Unsupported Type") || type.equals("Wildcard<?>")) {
                // preserve duplicates for unresolved types since they are
                // not actual types!
                unresolvedList.add(type);
            } else {
                customTypesSet.add(type); // ✅ Track unique custom types
            }
        }

        // convert Sets to Lists to maintain return format
        List<String> standardTypes = new ArrayList<>(standardTypesSet);
        List<String> customTypes = new ArrayList<>(customTypesSet);

        // return as a structured map
        Map<String, List<String>> result = new HashMap<>();
        result.put("standardTypes", standardTypes);
        result.put("customTypes", customTypes);
        result.put("unresolvedTypes", unresolvedList);

        return result;
    }
}
