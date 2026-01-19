package com.github.erfanarvan.methodanalyzerapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectProcessor {
    private final File projectDirectory;
    private final List<File> javaFiles;
    private final CSVWriter projectWriter;
    private final CSVWriter aggregatedWriter;

    private final CSVWriter projectSpecificWriter;


    // Anchor directory name
    private static final String APP_ROOT = "MethodAnalyzerApp";

    public ProjectProcessor(File projectDirectory, CSVWriter aggregatedWriter) {
        this.projectDirectory = projectDirectory;
        this.javaFiles = new ArrayList<>();
        this.projectWriter =
                new CSVWriter(projectDirectory.getName() + "_methods.csv");
        this.projectSpecificWriter =
                new CSVWriter(projectDirectory.getName() + "_project_specific_methods.csv");
        this.aggregatedWriter = aggregatedWriter;
    }


    public void process() {
        findJavaFiles(projectDirectory);
        for (File javaFile : javaFiles) {
            JavaFileProcessor processor =
                    new JavaFileProcessor(
                            javaFile,
                            projectWriter,
                            aggregatedWriter,
                            projectSpecificWriter
                    );
            processor.process();
        }
        projectWriter.close();
        projectSpecificWriter.close();
    }

    private void findJavaFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findJavaFiles(file);
            } else if (file.getName().endsWith(".java")) {
                if (isTestFile(file)) {
                    System.out.println("Skipping test file: " + file.getAbsolutePath());
                } else {
                    javaFiles.add(file);
                }
            }
        }
    }

    /**
     * A file is considered a test ONLY if "test" appears
     * AFTER the MethodAnalyzerApp directory in the path.
     */
    private boolean isTestFile(File file) {
        String path = file.getAbsolutePath().replace(File.separatorChar, '/');

        int anchorIdx = path.indexOf("/" + APP_ROOT + "/");
        if (anchorIdx == -1) {
            // Safety: if anchor not found, do NOT treat as test
            return false;
        }

        String relativePath = path.substring(anchorIdx + APP_ROOT.length() + 2)
                .toLowerCase();

        return relativePath.contains("/test/");
    }
}
