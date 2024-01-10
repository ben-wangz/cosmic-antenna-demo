plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
val slf4jVersion = "2.0.9"
var logbackVersion = "1.4.14"
val jacksonVersion = "2.13.4"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("io.minio:minio:8.5.7")
    implementation("commons-io:commons-io:2.12.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("ch.qos.logback:logback-core:$logbackVersion")

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
    manifest.attributes["Main-Class"] = "com.example.s3sync.Application"
}

var dockerSourceDir = project.file("docker").absolutePath
var dockerBuildDir = layout.buildDirectory.file("docker").get().asFile.absolutePath
tasks.register<org.gradle.api.tasks.Copy>("copyDocker") {
    dependsOn(tasks.shadowJar)
    group = "container"
    description = "copy resources to build/docker"
    doFirst {
        println("deleting docker build dir: $dockerBuildDir")
        delete(dockerBuildDir)
        println("copying resources($dockerBuildDir) to docker build dir($dockerBuildDir)")
    }
    from(dockerSourceDir)
    into(dockerBuildDir)
}
tasks.register<org.gradle.api.tasks.Copy>("copyJar") {
    dependsOn("copyDocker")
    group = "container"
    description = "copy resources to build/docker"
    from(tasks.shadowJar.get().archiveFile.get().asFile)
    into(dockerBuildDir)
}

var jarName = tasks.shadowJar.get().archiveFileName.get()
tasks.register<org.gradle.api.tasks.Exec>("buildImage") {
    dependsOn("copyJar")
    group = "container"
    description = "builds a container image for the project"
    commandLine(
        "podman", "build",
        "--build-arg", "JAR_NAME=$jarName",
        "-f", "$dockerBuildDir/Dockerfile",
        "-t", "${project.name}:$version",
        dockerBuildDir,
    )
    doFirst {
        println(commandLine.joinToString(" "))
    }
}
