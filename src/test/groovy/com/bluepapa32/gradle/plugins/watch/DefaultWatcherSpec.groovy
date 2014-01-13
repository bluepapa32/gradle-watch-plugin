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
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
    }

    def "register a directory which has no children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32").toPath())

        then:
        watcher.paths.size() == 1
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
    }

    def "register a file"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java/com/bluepapa32/Main.java").toPath())

        then:
        watcher.paths.size() == 1
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
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
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32").toPath())

        then:
        watcher.paths.size() == 2
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
    }

    def "unregister a directory which has children"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com").toPath())

        then:
        watcher.paths.size() == 1
        watcher.isWatching(file("src/main/java").toPath())
    }

    def "unregister a directory which is not watched"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32/hoge").toPath())

        then:
        watcher.paths.size() == 3
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
    }

    def "unregister a file"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())

        when:
        watcher.unregister(file("src/main/java/com/bluepapa32/Main.java").toPath())

        then:
        watcher.paths.size() == 3
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
    }

    def "is watcing the directory"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())

        then:
        watcher.isWatching(file("src/main/java").toPath())
        watcher.isWatching(file("src/main/java/com").toPath())
        watcher.isWatching(file("src/main/java/com/bluepapa32").toPath())
        !watcher.isWatching(file("src/main/java/com/bluepapa32/hoge").toPath())
    }

    def "close the watcher"() {
        given:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        watcher.register(file("src/main/java").toPath())
        then:
        watcher.paths.size() == 3

        when:
        watcher.close()
        then:
        watcher.paths.isEmpty()

        when:
        watcher.register(file("src/main/java").toPath())
        then:
        thrown(java.nio.file.ClosedWatchServiceException)

        when:
        watcher.unregister(file("src/main/java").toPath())
        then:
        thrown(java.nio.file.ClosedWatchServiceException)

        when:
        watcher.isWatching(file("src/main/java").toPath())
        then:
        notThrown(java.nio.file.ClosedWatchServiceException)

        when:
        watcher.take()
        then:
        thrown(java.nio.file.ClosedWatchServiceException)
    }
}

