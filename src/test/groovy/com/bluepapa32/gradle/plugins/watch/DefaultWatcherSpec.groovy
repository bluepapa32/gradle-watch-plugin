package com.bluepapa32.gradle.plugins.watch

import com.bluepapa32.gradle.plugins.GradlePluginSpecification
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class DefaultWatcherSpec extends GradlePluginSpecification {

    def "register a directory which has children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())
    }

    def "register a directory which has no children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32").toPath())

        then:
        watcher.paths.size() == 1
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())
    }

    def "register a file"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32/Main.java").toPath())

        then:
        watcher.paths.size() == 1
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())
    }

    def "register a direcotry which does not exist"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32/hoge").toPath())

        then:
        thrown(java.nio.file.NoSuchFileException)
    }

    def "register a file which does not exist"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32/Hoge.java").toPath())

        then:
        thrown(java.nio.file.NoSuchFileException)
    }

    def "register a file that the parent does not exist"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32/hoge/Hoge.java").toPath())

        then:
        thrown(java.nio.file.NoSuchFileException)
    }

    def "unregister a directory which has no children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32").toPath())

        then:
        watcher.paths.size() == 2
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
    }

    def "unregister a directory which has children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com").toPath())

        then:
        watcher.paths.size() == 1
        watcher.paths.contains(file("src/main/java").toPath())
    }

    def "unregister a directory which is not watched"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32/hoge").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())
    }

    def "unregister a file"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32/Main.java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file("src/main/java").toPath())
        watcher.paths.contains(file("src/main/java/com").toPath())
        watcher.paths.contains(file("src/main/java/com/bluepapa32").toPath())
    }
}

