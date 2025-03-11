package com.github.erfanarvan.methodanalyzerapp;

public class Main {
    public static void main(String[] args) {
        try {
            String directoryPath;
            if (args.length > 0) {
                directoryPath = args[0];
            } else {
                directoryPath = "repos";
            }

            System.out.println("Processing directory: " + directoryPath);
            DirectoryProcessor directoryProcessor = new DirectoryProcessor(directoryPath);
            directoryProcessor.processProjects();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
