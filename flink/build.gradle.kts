plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
var flinkVersion = "1.17.1"
val jacksonVersion = "2.13.4"
var slf4jVersion = "2.0.9"
var logbackVersion = "1.4.14"
val fabric8Version = "6.2.0"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("io.minio:minio:8.5.7")
    implementation("org.apache.flink:flink-walkthrough-common:$flinkVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.bytedeco:javacv-platform:1.5.9")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("io.fabric8:kubernetes-client:$fabric8Version")

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

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "com.example.flink.CosmicAntennaApp"
}

tasks.shadowJar{
    relocate("com.google.common", "com.example.flink.shadow.com.google.common")
}