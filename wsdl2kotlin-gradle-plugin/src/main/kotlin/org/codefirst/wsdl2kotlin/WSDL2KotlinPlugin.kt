package org.codefirst.wsdl2kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project

class WSDL2KotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("wsdl2kotlin") {
            println("Hello My Gradle Plugin!!")
        }
    }
}
