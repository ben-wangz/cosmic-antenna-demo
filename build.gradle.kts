plugins {
    id("com.diffplug.spotless") version "6.23.3" apply false
}

subprojects {    
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
    apply(plugin = "com.diffplug.spotless")
}