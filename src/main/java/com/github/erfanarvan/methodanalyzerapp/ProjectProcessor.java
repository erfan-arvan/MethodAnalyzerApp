package com.github.erfanarvan.methodanalyzerapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectProcessor {
    private final File projectDirectory;
    private final List<File> javaFiles;
    private final CSVWriter projectWriter;
    private final CSVWriter aggregatedWriter;

    public ProjectProcessor(File projectDirectory, CSVWriter aggregatedWriter) {
        this.projectDirectory = projectDirectory;
        this.javaFiles = new ArrayList<>();
        this.projectWriter = new CSVWriter(projectDirectory.getName() + "_methods.csv");
        this.aggregatedWriter = aggregatedWriter;
    }

    public void process() {
        findJavaFiles(projectDirectory);
        for (File javaFile : javaFiles) {
            JavaFileProcessor processor = new JavaFileProcessor(javaFile, projectWriter, aggregatedWriter);
            processor.process();
        }
        projectWriter.close();
    }

    private void findJavaFiles(File directory) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // skip directories that contain "test" in the name
                if (!file.getName().toLowerCase().contains("test")) {
                    findJavaFiles(file);
                }
            } else if (file.getName().endsWith(".java")) {
                // skip files that contain "test" in the path or name
                if (!file.getAbsolutePath().toLowerCase().contains("test")) {
                    javaFiles.add(file);
                } else {
                    System.out.println("Skipping test file: " + file.getAbsolutePath());
                }
            }
        }
    }

}
