package com.bluepapa32.gradle.plugins

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

abstract class GradlePluginSpecification extends Specification {

    def project = new ProjectBuilder().withProjectDir(new File('src/test/project')).build()

    def methodMissing(String name, args) {
        project."$name"(*args)
    }

    def propertyMissing(String name) {
        project."$name"
    }

    def propertyMissing(String name, value) {
        project."$name" = value
    }
}
