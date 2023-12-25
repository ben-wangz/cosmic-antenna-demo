import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
var flinkVersion = "1.17.1"
val jacksonVersion = "2.13.4"
var slf4jVersion = "2.0.9"
var logbackVersion = "1.4.14"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("org.apache.flink:flink-walkthrough-common:$flinkVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.bytedeco:javacv-platform:1.5.9")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    shadow("org.apache.flink:flink-streaming-java:$flinkVersion")
    shadow("org.apache.flink:flink-clients:$flinkVersion")
    shadow("ch.qos.logback:logback-classic:$logbackVersion")
    shadow("ch.qos.logback:logback-core:$logbackVersion")
    shadow(lombokDependency)

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlinGradle {
        target("**/*.kts")
        ktlint()
    }
    java {
        target("**/*.java")
        googleJavaFormat()
            .reflowLongStrings()
            .skipJavadocFormatting()
            .reorderImports(false)
    }
    yaml {
        target("**/*.yaml")
        jackson()
            .feature("ORDER_MAP_ENTRIES_BY_KEYS", true)
    }
    json {
        target("**/*.json")
        jackson()
            .feature("ORDER_MAP_ENTRIES_BY_KEYS", true)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        archiveVersion.set("1.0")
        archiveClassifier.set("")
        manifest {
            attributes(mapOf("Main-Class" to "com.example.helloworld.SensorApp"))
        }
        relocate("com.google.common", "com.example.helloworld.shadow.com.google.common")
    }
}
