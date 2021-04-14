package org.codefirst.wsdl2kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project

class WSDL2KotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("wsdl2kotlin", WSDL2KotlinPluginConfiguration::class.java)

        project.task("wsdl2kotlin") {
            it.doLast {
                val paths = extension.paths
                val outputs = WSDL2Kotlin().run(*paths.toTypedArray())
                outputs.forEach {
                    it.save(extension.outputDirectory ?: ".")
                }
            }
        }
    }
}
