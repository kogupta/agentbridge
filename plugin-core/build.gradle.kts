plugins {
    id("java")
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("info.solidsoft.pitest") version "1.19.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localPath = providers.gradleProperty("intellijPlatform.localPath").orNull?.takeIf { it.isNotBlank() }
        if (localPath != null) {
            local(localPath)
        } else {
            intellijIdeaUltimate(providers.gradleProperty("intellijPlatformVersion").get())
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains:annotations:${providers.gradleProperty("annotationsVersion").get()}")

    testImplementation("org.junit.jupiter:junit-jupiter:${providers.gradleProperty("junitVersion").get()}")
    testImplementation("net.jqwik:jqwik:${providers.gradleProperty("jqwikVersion").get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The IntelliJ Platform test framework registers com.intellij.tests.JUnit5TestSessionListener, which
    // loads JUnit 4 classes; without this the Gradle test executor fails to start.
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.catatafishen.ideagentforcopilot"
        name = "AgentBridge Native Agent"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    pitestVersion.set(providers.gradleProperty("pitestVersion"))
    junit5PluginVersion.set(providers.gradleProperty("pitestJunit5PluginVersion"))
    outputFormats.set(setOf("XML", "HTML"))
    timestampedReports.set(false)
    fullMutationMatrix.set(true)
    targetClasses.set(providers.gradleProperty("pitTargets").map { it.split(',').toSet() })
}
