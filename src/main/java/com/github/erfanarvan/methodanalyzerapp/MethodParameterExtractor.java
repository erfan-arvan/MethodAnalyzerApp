package com.github.erfanarvan.methodanalyzerapp;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.ast.body.Parameter;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MethodParameterExtractor {

    private static final int TIMEOUT_MS = 500; // Max time per parameter (milliseconds)

    /**
     * Resolves and retrieves the fully qualified types of all parameters in a given method.
     * <p>
     * This method processes each parameter of the given method and attempts to resolve
     * its type using JavaParser's symbol solver. The resolution runs within a timeout
     * to prevent hanging on complex type resolutions.
     * </p>
     *
     * @param method The method whose parameter types need to be resolved.
     * @return A list of fully qualified parameter types, or a fallback value (e.g., "Timeout" or "Unresolved") if resolution fails.
     */
    public static List<String> getResolvedParameterTypes(MethodDeclaration method) {
        return method.getParameters().stream()
                .map(MethodParameterExtractor::resolveTypeWithTimeout)
                .collect(Collectors.toList());
    }

    /**
     * Attempts to resolve the fully qualified type of a method parameter within a timeout.
     * <p>
     * This method ensures that type resolution does not block execution indefinitely
     * by running the resolution in a separate thread with a specified timeout. If the resolution
     * exceeds the allowed time, it is canceled and returns "Timeout".
     * </p>
     *
     * @param parameter The method parameter whose type needs to be resolved.
     * @return The resolved fully qualified type, or a fallback value if resolution times out or fails.
     */
    private static String resolveTypeWithTimeout(Parameter parameter) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> resolveType(parameter));

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return "Timeout";
        } catch (Exception e) {
            return "Unresolved";
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Resolves the fully qualified type of a given method parameter.
     * <p>
     * This method uses JavaParser's symbol solver to determine the parameter type
     * and return it in its fully qualified form. If the resolution fails due to an
     * exception, "Unresolved" is returned as a fallback.
     * </p>
     *
     * @param parameter The method parameter whose type needs to be resolved.
     * @return The fully qualified type of the parameter, or "Unresolved" if resolution fails.
     */
    private static String resolveType(Parameter parameter) {
        try {
            ResolvedType resolvedType = parameter.getType().resolve();
            return resolvedType.describe();
        } catch (Exception e) {
            // return "Unresolved" if resolution fails
            return "Unresolved";
        }
    }
}
