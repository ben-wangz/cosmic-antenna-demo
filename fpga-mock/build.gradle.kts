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

application {
    mainClass.set("com.example.fpga.FPGAMockClientApp")
}

val lombokDependency = "org.projectlombok:lombok:1.18.22"
val slf4jVersion = "2.0.9"
var logbackVersion = "1.4.14"
dependencies {
    annotationProcessor(lombokDependency)
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("io.netty:netty-all:4.1.101.Final")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

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
    enabled = true
    archiveBaseName.set("fpga-mock-client")
    archiveVersion.set("0.2")
    archiveClassifier.set("bundle")
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}
