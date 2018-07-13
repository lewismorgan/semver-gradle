import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.10"
    id("com.github.ben-manes.versions") version "0.20.0"
    id("org.jlleitschuh.gradle.ktlint") version "4.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7"
}

version = "0.9.7-beta"
group = "net.thauvin.erik.gradle"

var github = "https://github.com/ethauvin/semver-gradle"
var packageName = "net.thauvin.erik.gradle.semver"

var spekVersion = "1.1.5"

repositories {
    jcenter()
}

dependencies {
    compile(gradleApi())

    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())

    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.junit.platform")
    }

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.2.0") {
        because("Needed to run tests IDEs that bundle an older version")
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        // Gradle 4.6
        kotlinOptions.apiVersion = "1.2"
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek")
        }
    }

    val check by getting {
        dependsOn("ktlintCheck")
    }
}

detekt {
    profile("main", Action {
        input = "src/main/kotlin"
        filters = ".*/resources/.*,.*/build/.*"
        output = "$buildDir/reports/detekt-reports"
        outputName = "detekt-report"
        baseline = "detekt-baseline.xml"
    })
}

gradlePlugin {
    (plugins) {
        project.name {
            id = packageName
            implementationClass = "$packageName.SemverPlugin"
        }
    }
}

pluginBundle {
    website = github
    vcsUrl = github
    description = "Semantic Version Plugin for Gradle"
    tags = listOf("semver", "semantic", "version", "versioning", "auto-increment", "kotlin", "java")

    (plugins) {
        project.name {
            id = packageName
            displayName = project.name
        }
    }

    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
    }
}
