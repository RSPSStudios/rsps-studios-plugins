plugins {
    kotlin("jvm") version "1.5.21" apply false
    id("com.javatar.rsdeapi") version "1.0-SNAPSHOT" apply false
}

repositories {
    mavenCentral()
}

subprojects {
    group = "com.javatar.plugins"
    version = "1.0-SNAPSHOT"

    apply {
        plugin("kotlin")
        plugin("com.javatar.rsdeapi")
    }

    dependencies {
        "implementation"(kotlin("stdlib"))
        "testImplementation"(kotlin("test-junit5"))
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.6.0")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    extensions.configure<com.javatar.gradle.plugin.configurations.RspsStudiosPluginConfiguration>("rsdeplugin") {
        rsdeDirectory.set("/home/javatar/IdeaProjects/Intergrated-Development-Environment/application/build/image/application-linux")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        targetCompatibility = "15"
        sourceCompatibility = "15"
        kotlinOptions {
            jvmTarget = "15"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    targetCompatibility = "15"
}

tasks.register("runIDE") {
    val list = subprojects.map { it.tasks.named("assemblePlugin") }
    dependsOn(*list.toTypedArray())
}