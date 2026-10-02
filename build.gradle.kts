import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.Container
import org.gradle.plugins.ide.eclipse.model.Library
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

plugins {
    java
    eclipse
    checkstyle
    pmd
    id("com.github.spotbugs") version "6.5.10"
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources {
            setSrcDirs(listOf("src"))
            exclude("**/*.java")
        }
    }
    test {
        java.setSrcDirs(listOf("tst"))
        resources {
            setSrcDirs(listOf("tst"))
            exclude("**/*.java")
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Eclipse without Buildship: copy dependency jars into lib/ and point .classpath there.
val copyLibs by tasks.registering(Sync::class) {
    description = "Copies dependency jars into lib/ for a non-Buildship Eclipse setup."
    group = "ide"
    from(configurations.compileClasspath)
    from(configurations.runtimeClasspath)
    from(configurations.testCompileClasspath)
    from(configurations.testRuntimeClasspath)
    into(layout.projectDirectory.dir("lib"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

eclipse {
    project {
        natures.remove("org.eclipse.buildship.core.gradleprojectnature")
        buildCommands.removeIf {
            it.name == "org.eclipse.buildship.core.gradleprojectbuilder"
        }
    }
    classpath {
        file {
            whenMerged {
                val classpath = this as Classpath
                classpath.entries.removeIf {
                    it is Container && it.path.contains("buildship")
                }
                classpath.entries
                    .filterIsInstance<Library>()
                    .forEach { lib ->
                        lib.path = "lib/" + File(lib.path).name
                        lib.sourcePath = null
                        val scope = lib.entryAttributes["gradle_used_by_scope"]?.toString()
                        if (scope.isNullOrEmpty()) {
                            lib.entryAttributes["gradle_used_by_scope"] = "test"
                        }
                    }
            }
        }
    }
}

tasks.named("eclipseClasspath") {
    dependsOn(copyLibs)
}

tasks.named("eclipseJdt") {
    doLast {
        val prefs = layout.projectDirectory.file(".settings/org.eclipse.jdt.core.prefs").asFile
        if (prefs.isFile) {
            val cleaned = prefs.readLines()
                .dropWhile { it == "#" || it.matches(Regex("#[A-Z][A-Za-z]{2} .* \\d{4}")) }
            prefs.writeText(cleaned.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        }
    }
}

checkstyle {
    toolVersion = "10.18.0"
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    isShowViolations = true
}

pmd {
    toolVersion = "7.26.0"
    isConsoleOutput = true
    isIgnoreFailures = false
    ruleSets = emptyList()
    ruleSetConfig = resources.text.fromFile(file("${rootDir}/config/pmd/pmd.xml"))
}

spotbugs {
    toolVersion = "4.10.3"
    ignoreFailures.set(false)
    excludeFilter.set(file("${rootDir}/config/spotbugs/exclude.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
    }
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    }
}

tasks.named("check") {
    dependsOn(
        tasks.named("checkstyleMain"),
        tasks.named("pmdMain"),
        tasks.named("spotbugsMain"),
        tasks.named("jacocoTestReport")
    )
}
