package com.github.erfanarvan.methodanalyzerapp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.*;
import java.util.concurrent.*;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodExtractor {
    private final File javaFile;
    private final CompilationUnit cu;
    private final CSVWriter projectWriter;
    private final CSVWriter aggregatedWriter;
    private String packageName = "";

    private static final int TIMEOUT_MS = 1000;
    private static final int TIMEOUT_MS_TYPE = 500;

    /**
     * Initializes a MethodExtractor to analyze and extract information from methods
     * within a given Java file.
     *
     * @param javaFile         The Java source file being processed.
     * @param cu               The parsed CompilationUnit representing the Java file.
     * @param projectWriter    The CSV writer used to store method data for the current project.
     * @param aggregatedWriter The CSV writer used to store aggregated method data across projects.
     */
    public MethodExtractor(File javaFile, CompilationUnit cu, CSVWriter projectWriter, CSVWriter aggregatedWriter) {
        this.javaFile = javaFile;
        this.cu = cu;
        this.projectWriter = projectWriter;
        this.aggregatedWriter = aggregatedWriter;
        this.packageName = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");
    }

    /**
     * Extracts and processes all methods from the Java file.
     * <p>
     * This method iterates through all classes and interfaces found in the
     * parsed CompilationUnit (`cu`) and processes each method within them.
     * It ensures that each method is analyzed, and its relevant information
     * is written to both the project-specific and aggregated CSV files.
     * </p>
     */
    public void extract() {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            String className = clazz.getNameAsString();
            clazz.findAll(MethodDeclaration.class).forEach(method -> processMethodWithTimeout(className, method));
        });
    }

    /**
     * Processes a method with a timeout to prevent long-running computations.
     * <p>
     * This method runs `processMethod` in a separate thread and enforces a timeout
     * to prevent JavaParser from hanging if method analysis takes too long.
     * If the method processing exceeds the defined timeout, it is canceled
     * to ensure the program continues running efficiently.
     * </p>
     *
     * @param className The name of the class containing the method.
     * @param method    The method declaration to be processed.
     */
    private void processMethodWithTimeout(String className, MethodDeclaration method) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> processMethod(className, method));

        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.println("Timeout processing method: " + method.getNameAsString());
        } catch (Exception e) {
            System.err.println("Error processing method: " + method.getNameAsString() + " - " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Extracts and processes detailed information from a given method.
     * <p>
     * This method analyzes various properties of the method, including its name, return type,
     * parameters, access modifiers, annotations, Javadoc, and expressions used within it.
     * It also categorizes expression types into standard, custom, and unresolved types.
     * Additionally, it counts the number of lines in the method body, both raw and without comments.
     * <p>
     * The extracted information is formatted and written to both the project-specific
     * and aggregated CSV files.
     * </p>
     *
     * @param className The name of the class containing the method.
     * @param method    The method declaration being processed.
     */
    private void processMethod(String className, MethodDeclaration method) {
        String methodName = method.getNameAsString();
        String returnType = method.getTypeAsString();
        int numParams = method.getParameters().size();

        List<String> parameterTypes = MethodParameterExtractor.getResolvedParameterTypes(method);
        Map<String, List<String>> parTypeCounts =
                TypeCounter.countStandardAndCustomTypes(parameterTypes);

        List<String> parStandardTypes = parTypeCounts.get("standardTypes");
        List<String> parCustomTypes = parTypeCounts.get("customTypes");
        List<String> parUnresolvedTypes = parTypeCounts.get("unresolvedTypes");

        String accessModifier = method.getAccessSpecifier().asString();
        String annotations = method.getAnnotations().stream()
                .map(a -> a.getNameAsString())
                .collect(Collectors.joining(", "));
        String isStatic = String.valueOf(method.isStatic());
        String isDefault = String.valueOf(method.isDefault());
        String isFinal = String.valueOf(method.isFinal());
        String isAbstract = String.valueOf(method.isAbstract());


        String javadoc = getJavadocText(method);

        List<String> expressionTypes = method.findAll(Expression.class).stream()
                .filter(exp -> !(exp instanceof AnnotationExpr))  // Ignore annotations (@Override, @NonNull)
                .filter(exp -> !(exp instanceof LiteralExpr))      // Ignore literals (false, 0, "text", null)
                .map(this::resolveExpressionType)
                .filter(type -> !type.isEmpty())
                .collect(Collectors.toList());

        Map<String, List<String>> exTypeCounts =
                TypeCounter.countStandardAndCustomTypes(expressionTypes);

        List<String> exStandardTypes = exTypeCounts.get("standardTypes");
        List<String> exCustomTypes = exTypeCounts.get("customTypes");
        List<String> exUnresolvedTypes = exTypeCounts.get("unresolvedTypes");

        //Convert the list to set to only count the unique types for standard
        // types
        Set<String> setStandard = Stream.concat(parStandardTypes.stream(),
                        exStandardTypes.stream())
                .collect(Collectors.toSet());

        // Convert to set to only count the unique types for custom types,
        // but we do not need to do this for unresolved ones since we
        // consider any of unresolved ones as a separate type.
        Set<String> setCustom = Stream.concat(parCustomTypes.stream(),
                        exCustomTypes.stream())
                .collect(Collectors.toSet());
        if(TypeUtils.isStandardType(returnType)){
            setStandard.add(returnType);
        } else setCustom.add(returnType);

        //Num of all unique standard types of parameters, expressions, and
        // possibly return type if it's standard
        int allStandardCount = setStandard.size();

        //Num of all unique custom types of parameters, expressions, and
        // possibly return type if it's standard
        int allCustomsCount =
                setCustom.size() + parUnresolvedTypes.size()+exUnresolvedTypes.size();

        int[] lineCounts = MethodLineCounter.countMethodLines(method);
        int rawLoc = lineCounts[0];
        int cleanLoc = lineCounts[1];

        //System.out.println(" Expression types: " + expressionTypes.toString
        // () + "\nfor " + method.findAll(Expression.class).toString());
        String csvLine = String.join(",",
                methodName, javaFile.getPath(), className, packageName,
                CSVWriter.sanitizeForCSV(returnType),
                isFinal, isAbstract, isDefault, isStatic,
                CSVWriter.sanitizeForCSV(String.valueOf(numParams)),
                CSVWriter.sanitizeForCSV(parameterTypes.toString()),
                String.valueOf(parStandardTypes.size()),
                String.valueOf(parCustomTypes.size()),
                String.valueOf(parUnresolvedTypes.size()),
                CSVWriter.sanitizeForCSV(accessModifier),
                CSVWriter.sanitizeForCSV(annotations),
                CSVWriter.sanitizeForCSV(javadoc),
                CSVWriter.sanitizeForCSV(method.findAll(Expression.class).toString()),
                CSVWriter.sanitizeForCSV(expressionTypes.toString()),
                String.valueOf(exStandardTypes.size()),
                String.valueOf(exCustomTypes.size()),
                String.valueOf(exUnresolvedTypes.size()),
                String.valueOf(rawLoc),
                String.valueOf(cleanLoc),
                String.valueOf(allStandardCount),
                String.valueOf(allCustomsCount)
        );

        projectWriter.write(csvLine);
        aggregatedWriter.write(csvLine);
    }

    /**
     * Extracts the Javadoc comment associated with the given method.
     * <p>
     * This method first attempts to retrieve structured Javadoc using JavaParser's
     * `getJavadoc()` method. If no structured Javadoc is found, it checks for
     * a regular comment and parses it as Javadoc if possible. If no Javadoc is
     * found, it returns an empty string.
     * </p>
     *
     * @param method The method declaration from which to extract Javadoc.
     * @return The extracted Javadoc description as a string, or an empty string if no Javadoc is found.
     */
    private String getJavadocText(MethodDeclaration method) {
        // try to get structured Javadoc (best approach)
        if (method.getJavadoc().isPresent()) {
            return method.getJavadoc().get().getDescription().toText();
        }

        // if Javadoc is not present via `getJavadoc()`, check for regular
        // comments
        if (method.getComment().isPresent()) {
            var comment = method.getComment().get();
            if (comment instanceof JavadocComment) {
                return ((JavadocComment) comment).parse().getDescription().toText();
            }
        }

        //  if no Javadoc is found, return an empty string
        return "";
    }
    /**
     * Resolves the type of a given expression.
     * <p>
     * This method attempts to determine the fully qualified type of the provided expression
     * using JavaParser's symbol solver. It handles various expression types, including
     * primitive types, arrays, reference types, type variables, and wildcards.
     * </p>
     * <p>
     * The resolution process runs within a separate thread with a timeout to prevent
     * indefinite blocking. If resolution fails due to an `UnsolvedSymbolException`,
     * `UnsupportedOperationException`, or timeout, a fallback value such as
     * "Unresolved", "Unsupported Type", or "Timeout" is returned.
     * </p>
     *
     * @param exp The expression whose type needs to be resolved.
     * @return A string representing the resolved type of the expression, or a fallback value if resolution fails.
     */
    private String resolveExpressionType(Expression exp) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(() -> {
                try {
                    ResolvedType resolvedType = exp.calculateResolvedType();

                    if (resolvedType.isPrimitive()) return resolvedType.asPrimitive().describe();
                    if (resolvedType.isArray()) return resolvedType.asArrayType().describe();
                    if (resolvedType.isReferenceType()) {
                        ResolvedReferenceType refType = resolvedType.asReferenceType();
                        return refType.getQualifiedName();  // ✅ Ensures fully qualified names
                    }
                    if (resolvedType.isTypeVariable()) return "Generic<" + resolvedType.asTypeVariable().describe() + ">";
                    if (resolvedType instanceof ResolvedWildcard) return "Wildcard<?>";

                    return resolvedType.describe();
                } catch (UnsolvedSymbolException e) {
                    return "Unresolved";
                } catch (UnsupportedOperationException e) {
                    return "Unsupported Type";
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            });

            try {
                return future.get(TIMEOUT_MS_TYPE, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return "Timeout";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            } finally {
                executor.shutdown();
            }
    }


}
