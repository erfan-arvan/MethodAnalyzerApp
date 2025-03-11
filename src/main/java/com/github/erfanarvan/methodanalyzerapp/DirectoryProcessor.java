package com.github.erfanarvan.methodanalyzerapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryProcessor {
    private final File rootDirectory;
    private final List<File> projects;
    private final CSVWriter aggregatedWriter;
    private static final String AGGREGATED_CSV = "aggregated_methods.csv";

    /**
     * Initializes a `DirectoryProcessor` to analyze multiple Java projects within a root directory.
     * <p>
     * This constructor sets up the processing environment by identifying project directories
     * and ensuring a fresh aggregated CSV file is created for storing method data across projects.
     * If an existing aggregated CSV file is found, it is deleted before creating a new one.
     * </p>
     *
     * @param rootPath The path to the root directory containing multiple Java projects.
     */
    public DirectoryProcessor(String rootPath) {
        this.rootDirectory = new File(rootPath);
        this.projects = new ArrayList<>();

        // check if the aggregated CSV file exists, and delete it
        File aggregatedFile = new File(AGGREGATED_CSV);
        if (aggregatedFile.exists()) {
            if (aggregatedFile.delete()) {
                System.out.println("Deleted existing aggregated CSV file: " + AGGREGATED_CSV);
            } else {
                System.err.println("Failed to delete aggregated CSV file.");
            }
        }

        // now create a new aggregated CSV file
        this.aggregatedWriter = new CSVWriter(AGGREGATED_CSV);
        System.out.println("Created new aggregated CSV file: " + AGGREGATED_CSV);
    }


    /**
     * Processes all Java projects found in the root directory.
     * <p>
     * This method scans the root directory for project folders, initializes a `ProjectProcessor`
     * for each project, and starts the analysis. Once all projects are processed, the aggregated
     * CSV writer is closed.
     * </p>
     */
    public void processProjects() {
        findProjects(rootDirectory);
        for (File project : projects) {
            System.out.println("Processing project: " + project.getName());
            ProjectProcessor projectProcessor = new ProjectProcessor(project, aggregatedWriter);
            projectProcessor.process();
        }
        aggregatedWriter.close();
        System.out.println("Processing completed.");
    }

    /**
     * Identifies project directories within the given root directory.
     * <p>
     * This method scans the specified directory for subdirectories, assuming that each
     * subdirectory represents an individual Java project. These subdirectories are added
     * to the `projects` list for further processing.
     * </p>
     *
     * @param directory The root directory containing multiple Java projects.
     */
    private void findProjects(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    projects.add(file);
                }
            }
        }
    }
}
