plugins {
    kotlin("jvm") apply(true)
}



dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api(project(":sdk"))

}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
