dependencies {
    implementation(kotlin("stdlib", extra["kotlin_version"] as String))
    implementation("io.atomix.catalyst", "catalyst-transport", extra["catalyst_version"] as String)
    implementation("io.atomix.catalyst", "catalyst-concurrent", extra["catalyst_version"] as String)
    implementation("io.atomix.catalyst", "catalyst-serializer", extra["catalyst_version"] as String)
    implementation("io.github.microutils", "kotlin-logging", extra["kotlinlog_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-launcher", extra["junit-platform_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-runner", extra["junit-platform_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-engine", extra["junit-platform_version"] as String)
    testRuntime("org.junit.jupiter", "junit-jupiter-engine", extra["junit-jupiter_version"] as String)
    testCompile("org.junit.jupiter", "junit-jupiter-api", extra["junit-jupiter_version"] as String)
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}