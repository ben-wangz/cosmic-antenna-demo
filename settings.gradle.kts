plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "flink-learning"
include("flink")
include("fpga-mock")
