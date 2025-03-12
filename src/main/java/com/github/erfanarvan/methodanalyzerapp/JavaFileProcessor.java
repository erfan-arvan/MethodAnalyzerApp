package com.github.erfanarvan.methodanalyzerapp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.concurrent.*;

public class JavaFileProcessor {
    private final File javaFile;
    private final CSVWriter projectWriter;
    private final CSVWriter aggregatedWriter;
    private static final int PARSE_TIMEOUT_MS = 5000; // ⏳ 5 seconds timeout for parsing

    /**
     * Initializes a JavaFileProcessor to analyze and extract method information from a Java source file.
     * <p>
     * This class is responsible for parsing the given Java file, setting up a symbol solver for type resolution,
     * and passing the parsed `CompilationUnit` to the `MethodExtractor` for method analysis.
     * </p>
     *
     * @param javaFile         The Java source file to be processed.
     * @param projectWriter    The CSV writer used to store method data for the current project.
     * @param aggregatedWriter The CSV writer used to store aggregated method data across multiple projects.
     */
    public JavaFileProcessor(File javaFile, CSVWriter projectWriter, CSVWriter aggregatedWriter) {
        this.javaFile = javaFile;
        this.projectWriter = projectWriter;
        this.aggregatedWriter = aggregatedWriter;
    }


    /**
     * Parses the Java source file and extracts method details.
     * <p>
     * This method sets up a symbol solver for type resolution, parses the Java file into a `CompilationUnit`,
     * and then delegates the extraction of method information to the `MethodExtractor` class. If parsing fails,
     * an error message is printed to indicate the issue.
     * </p>
     */
    public void process() {
        System.out.println("Parsing file: " + javaFile.getAbsolutePath());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CompilationUnit> future = executor.submit(this::parseFile);

        try {
            // Get the result within the timeout, otherwise, cancel it
            CompilationUnit cu = future.get(PARSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (cu != null) {
                // Pass the parsed file to MethodExtractor
                MethodExtractor extractor = new MethodExtractor(javaFile, cu, projectWriter, aggregatedWriter);
                extractor.extract();
            }

        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.println("Timeout parsing file: " + javaFile.getName());
        } catch (Exception e) {
            System.err.println("Error parsing file: " + javaFile.getName() + " - " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private CompilationUnit parseFile() {
        try {
            // Configure JavaParser with a Symbol Solver (Before Parsing)
            TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
            TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(javaFile.getParent()));

            CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
            combinedSolver.add(reflectionTypeSolver);   // standard Java
            // types (int, String, etc.)
            combinedSolver.add(javaParserTypeSolver);   // project-specific
            // types (MyClass, etc.)

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
            // Apply Symbol Solver
            StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

            // Parse the Java File
            return StaticJavaParser.parse(javaFile);

        } catch (Exception e) {
            System.err.println("Error parsing file: " + javaFile.getName() + " - " + e.getMessage());
            return null;
        }
    }
}