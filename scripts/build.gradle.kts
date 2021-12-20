plugins {
    kotlin("jvm") apply(true)
}



dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api(project(":sdk"))
    implementation("org.yaml:snakeyaml:1+")

}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
