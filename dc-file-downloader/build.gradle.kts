plugins {
    id("java")
    application
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

application{
    mainClass.set("com.example.fpga.ClientApp")
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
val slf4jVersion = "2.0.9"
var logbackVersion = "1.4.14"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("io.minio:minio:8.5.7")
    implementation("commons-io:commons-io:2.12.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")

    shadow("ch.qos.logback:logback-classic:${logbackVersion}")
    shadow("ch.qos.logback:logback-core:${logbackVersion}")
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
    enabled = true
    archiveBaseName.set("dc-file-downloader")
    archiveVersion.set("0.1")
    archiveClassifier.set("bundle")
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}