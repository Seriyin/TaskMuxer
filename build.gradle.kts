subprojects {
    group = "pt.um.tf.taskmux"
    version = "1.0-SNAPSHOT"

    apply {
        plugin("java")
    }

    extra["junit-jupiter_version"] = "5.2.0"
    extra["junit-platform_version"] = "1.2.0"
    extra["spread_version"] = "4.4.0"
    extra["catalyst_version"] = "1.2.1"
    extra["ekit_version"] = "1.2-SNAPSHOT"
    extra["slf4j_version"] = "1.8.0-beta2"

    repositories {
        mavenCentral()
        mavenLocal()
    }


}

