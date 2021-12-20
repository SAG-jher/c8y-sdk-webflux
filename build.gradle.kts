plugins {
    kotlin("jvm") version "1.5.31" apply false
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.2"
}

group = "io.c8y"
version = "1.0-SNAPSHOT"
allprojects{
    repositories {
        mavenCentral()
    }
}
//subprojects {
//    tasks {
//        named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
//            kotlinOptions {
//                jvmTarget = "11"
//            }
//        }
//    }
//}
