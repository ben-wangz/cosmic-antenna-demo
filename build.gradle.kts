plugins {
    id("com.diffplug.spotless") version "6.23.3"
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
        targetExclude(".vscode/settings.json")
        jackson()
            .feature("ORDER_MAP_ENTRIES_BY_KEYS", true)
    }
}

allprojects {
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
    ext {
        version = "1.0.3"
    }
}
