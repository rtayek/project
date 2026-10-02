# project

Template for Ray's Gradle + Eclipse Java projects.

This repository is intended to remain a valid, buildable reference project. Gradle is the authoritative build system. Eclipse metadata is generated with the Gradle `eclipse` plugin, but the generated project does not depend on Buildship.

## Standard verification

```sh
./gradlew eclipse check
```

`check` includes:

- JUnit tests
- Checkstyle
- PMD
- SpotBugs
- JaCoCo coverage report

JaCoCo HTML output is written under `build/reports/jacoco/html`.

## Source layout

```text
src/    production Java and resources
tst/    test Java and resources
config/ static-analysis configuration
lib/    generated dependency jars for Eclipse; not committed
```

The template package is `org.ray.template`. `new-gradle-eclipse-project.sh` replaces it with the requested package and replaces the Gradle project name `gradle-eclipse-template` with the new project name.

## Eclipse without Buildship

Run:

```sh
./gradlew eclipse
```

The build copies required dependency jars into `lib/`, removes the Buildship nature/container, and generates ordinary Eclipse `.project`, `.classpath`, and `.settings/` metadata. Import with:

```text
File -> Import -> General -> Existing Projects into Workspace
```

Leave `Copy projects into workspace` unchecked.

## Creating a project

Keep the template checked out at:

```text
~/eclipse-workspace/project
```

Then run, for example:

```sh
new-gradle-eclipse-project.sh ~/eclipse-workspace/hello org.ray.hello
```

The generator copies the template, removes repository/build/IDE output, changes the project and package names, and verifies the result with `./gradlew eclipse check`.
