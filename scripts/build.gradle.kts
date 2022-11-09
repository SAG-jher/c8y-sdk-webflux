plugins {
    kotlin("jvm") apply(true)
}



dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api(project(":sdk"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.yaml:snakeyaml:1.32")
    implementation("org.apache.commons:commons-lang3:3.12.0")

}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
