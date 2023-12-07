import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven { setUrl("https://maven.aliyun.com/repository/public") }
    maven { setUrl("https://maven.aliyun.com/repository/spring") }
    maven { setUrl("https://maven.aliyun.com/repository/mapr-public") }
    maven { setUrl("https://maven.aliyun.com/repository/spring-plugin") }
    maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { setUrl("https://maven.aliyun.com/repository/google") }
    maven { setUrl("https://maven.aliyun.com/repository/jcenter") }
    mavenCentral()
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
var flinkVersion = "1.17.1"
val jacksonVersion = "2.13.4"
var log4jVersion = "2.17.1"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("org.apache.flink:flink-walkthrough-common:${flinkVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("org.bytedeco:javacv-platform:1.5.9")
    shadow("org.apache.flink:flink-streaming-java:${flinkVersion}")
    shadow("org.apache.flink:flink-clients:${flinkVersion}")
    shadow("org.apache.logging.log4j:log4j-slf4j-impl:${log4jVersion}")
    shadow("org.apache.logging.log4j:log4j-api:${log4jVersion}")
    shadow("org.apache.logging.log4j:log4j-core:${log4jVersion}")
    shadow(lombokDependency)

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
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