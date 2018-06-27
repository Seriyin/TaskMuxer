apply {
    plugin("application")
}


dependencies {
    implementation("pt.haslab", "ekit", extra["ekit_version"] as String)
    implementation("org.slf4j", "slf4j-api", extra["slf4j_version"] as String)
    implementation("org.slf4j", "slf4j-simple", extra["slf4j_version"] as String)
    implementation("io.github.microutils", "kotlin-logging", extra["kotlinlog_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-launcher", extra["junit-platform_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-runner", extra["junit-platform_version"] as String)
    testRuntime("org.junit.platform", "junit-platform-engine", extra["junit-platform_version"] as String)
    testRuntime("org.junit.jupiter", "junit-jupiter-engine", extra["junit-jupiter_version"] as String)
    testCompile("org.junit.jupiter", "junit-jupiter-api", extra["junit-jupiter_version"] as String)
    implementation(project(":muxer-commons"))
}

configure<ApplicationPluginConvention> {
    mainClassName = "pt.um.lei.tf.taskmux.server.MainKt"
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}