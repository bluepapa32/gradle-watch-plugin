package com.bluepapa32.gradle.plugins.watch

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class WatchPluginSpec extends Specification {

    def project = new ProjectBuilder().build()

    def "applies plugin"() {
        when:
        apply plugin: 'watch'

        then:
        tasks['watch'] instanceof WatchTask
    }

    def "configures watch"() {
        setup:
        apply plugin: 'watch'

        when:
        watch {
            main {
            }
            test {
            }
        }

        then:
        tasks['watch'] instanceof WatchTask
        tasks['watch'].targets.size() == 2

        then:
        tasks['watchMain'] instanceof WatchTask
        tasks['watchMain'].targets.size() == 1
        tasks['watchMain'].targets*.name == ['main']

        then:
        tasks['watchTest'] instanceof WatchTask
        tasks['watchTest'].targets.size() == 1
        tasks['watchTest'].targets*.name == ['test']
    }

//  ----------------------------------------------------------------------------
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
