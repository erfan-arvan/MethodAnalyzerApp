# methodanalyzerapp

Method Analyzer App is a Java tool that analyzes Java source code files to extract detailed information about methods. It uses **JavaParser** to parse Java files and outputs method details into CSV files for easy inspection.  

## Features  
Extracts method details such as:  
- Method name, class name, package  
- Return type, parameters, access modifiers  
- Annotations and Javadoc  
- Expressions and their resolved types  
- Number of lines of code (with and without comments)  

## Installation  

### **Clone the Repository**  
```sh
git clone https://github.com/yourusername/MethodAnalyzerApp.git
cd MethodAnalyzerApp

# Ensure you have Gradle and JDK 17 installed
./gradlew build
