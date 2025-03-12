plugins {
    id("java")
    id ("application")
}

group = "com.github.erfanarvan.methodanalyzerapp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.github.javaparser:javaparser-core:3.25.2")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.2")
    implementation("com.google.guava:guava:31.1-jre")
    implementation ("com.google.googlejavaformat:google-java-format:1.17.0")
    implementation ("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}
application {
    mainClass.set("com.github.erfanarvan.methodanalyzerapp.Main")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    test {
        java {
            setSrcDirs(listOf("src/test/java"))
        }
    }
}

