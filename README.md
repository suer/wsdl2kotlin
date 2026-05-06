# WSDL2Kotlin

A stub generator for services implemented by WSDL.

Inspired by [WSDL2Swift](https://github.com/banjun/WSDL2Swift)

## Usage

Add maven repository to `settings.gradle`:

```gradle:settings.gradle
pluginManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/suer/wsdl2kotlin")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("USERNAME") ?: "suer"
                password = project.findProperty("gpr.key") ?: System.getenv("TOKEN") ?: ""
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/suer/wsdl2kotlin")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("USERNAME") ?: "suer"
                password = project.findProperty("gpr.key") ?: System.getenv("TOKEN") ?: ""
            }
        }
        mavenCentral()
    }
}
```

Then, add plugin configuration to `build.gradle`:

```gradle:build.gradle
plugins {
    id 'org.codefirst.wsdl2kotlin.wsdl2kotlin-gradle-plugin' version '0.7.0'
}

wsdl2kotlin {
    // path to WSDL and XSD files
    paths = ["app/src/test/resources/sample.wsdl.xml", "app/src/test/resources/sample.xsd.xml"]
    // path to output destination directory of generated source code
    outputDirectory = "app/src/main/kotlin/"
}
```

Generate codes by:

```
$ ./gradlew wsdl2kotlin
```

Finally, append `wsdl2kotlin-runtime` to `app/build.gradle`:

```gradle:app/build.gradle
dependencies {
    implementation 'org.codefirst.wsdl2kotlin:wsdl2kotlin-runtime:0.7.0'
}
```

## For developers

### Build


```
$ ./gradlew build
```

### Test


```
$ ./gradlew test
```

### Lint

check your source code:

```
$ ./gradlew ktlint
```

format all source code automatically:

```
$ ./gradlew ktFormat
```
