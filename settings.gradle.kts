plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "flink-learning"
include("opencv")
include("flink")
include("fpga-mock")
