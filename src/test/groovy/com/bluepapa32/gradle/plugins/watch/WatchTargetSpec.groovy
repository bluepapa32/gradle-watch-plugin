package com.bluepapa32.gradle.plugins.watch

import com.bluepapa32.gradle.plugins.GradlePluginSpecification
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class WatchTargetSpec extends GradlePluginSpecification {

    def "add some tasks"() {
        given:
        WatchTarget target = new WatchTarget()

        when: target.tasks 'foo'
        then: target.tasks == [ 'foo' ]

        when: target.tasks 'bar', 'hoge'
        then: target.tasks == [ 'foo', 'bar', 'hoge' ]
    }

    def "Some files are specified by FileTree"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files fileTree(dir: 'src/main/java', include: '**/*.java')

        when:
        File file = file(path)

        then:
        target.isTarget(file.toPath()) == expected

        where:
        path                                       | expected
        "hoge"                                     | false
        "src"                                      | false
        "src/main"                                 | false
        "src/main/java"                            | false
        "src/main/java/com/bluepapa32/Main.java"   | true
        "src/main/java/com/bluepapa32/Hoge.java"   | true     // not exist
        "src/main/java/com/bluepapa32/Main.groovy" | false    // not match

    }

    def "A directory are specified by FileCollection"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java')

        when:
        File file = file(path)

        then:
        target.isTarget(file.toPath()) == expected

        where:
        path                                         | expected
        "hoge"                                       | false
        "src"                                        | false
        "src/main"                                   | false
        "src/main/java"                              | true
        "src/main/java/com/bluepapa32/Main.java"     | true
        "src/main/java/com/bluepapa32/Hoge.java"     | true    // not exist
        "src/test/java/com/bluepapa32/MainTest.java" | false   // not match
    }

    def "A file are specified by FileCollection"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/Main.java')

        when:
        File file = file(path)

        then:
        target.isTarget(file.toPath()) == expected

        where:
        path                                         | expected
        "hoge"                                       | false
        "src"                                        | false
        "src/main"                                   | false
        "src/main/java"                              | false
        "src/main/java/com/bluepapa32/Main.java"     | true
        "src/main/java/com/bluepapa32/Hoge.java"     | false   // not match
        "src/test/java/com/bluepapa32/MainTest.java" | false   // not match
    }

    def "Some files are specified by FileCollection"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/Main.java', 'src/main/java/com/bluepapa32/Hoge.java', 'src/test/java')

        when:
        File file = file(path)

        then:
        target.isTarget(file.toPath()) == expected

        where:
        path                                          | expected
        "hoge"                                        | false
        "src"                                         | false
        "src/main"                                    | false
        "src/main/java"                               | false
        "src/main/java/com/bluepapa32/Main.java"      | true
        "src/main/java/com/bluepapa32/Hoge.java"      | true       // not exist
        "src/main/java/com/bluepapa32/Foo.java"       | false      // not match
        "src/test/java/com/bluepapa32/MainTest.java"  | true
        "src/test/java/com/bluepapa32/HogeTest.java"  | true       // not exist
        "src/test/java/com/bluepapa32/FooTest.groovy" | true       // not match
    }

    def "Some files are specified by FileCollection and FileTree"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/Main.java', 'src/main/java/com/bluepapa32/Hoge.java')
        target.files fileTree(dir: 'src/test/java', include: '**/*.java')

        when:
        File file = file(path)

        then:
        target.isTarget(file.toPath()) == expected

        where:
        path                                          | expected
        "hoge"                                        | false
        "src"                                         | false
        "src/main"                                    | false
        "src/main/java"                               | false
        "src/main/java/com/bluepapa32/Main.java"      | true
        "src/main/java/com/bluepapa32/Hoge.java"      | true    // not exist
        "src/main/java/com/bluepapa32/Foo.java"       | false   // not match
        "src/test/java/com/bluepapa32/MainTest.java"  | true
        "src/test/java/com/bluepapa32/HogeTest.java"  | true    // not exist
        "src/test/java/com/bluepapa32/FooTest.groovy" | false   // not match
    }

    def "return true if the file is up to date"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java')

        and:
        File file = file("src/main/java/com/bluepapa32/Main.java")

        expect: file.exists()

        // up to date
        when: target.executedAt = 0
        then: target.isTarget(file.toPath())

        when: target.executedAt = file.lastModified() - 1
        then: target.isTarget(file.toPath())

        // not up to date
        when: target.executedAt = file.lastModified()
        then: !target.isTarget(file.toPath())

        when: target.executedAt = file.lastModified() + 1
        then: !target.isTarget(file.toPath())
    }

    def "return true if the file does not exist"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java')

        and:
        File file = file("src/main/java/com/bluepapa32/Hoge.java")

        expect: !file.exists()

        when: target.executedAt = 0
        then: target.isTarget(file.toPath())

        when: target.executedAt = System.currentTimeMillis()
        then: target.isTarget(file.toPath())
    }

//  ------------------------------------------------------------------------------

    def "watch one file"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/Main.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 1
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
    }

    def "watch more than one file"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/Main.java',
                           'src/test/java/com/bluepapa32/MainTest.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 2
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
        watcher.paths.contains(file('src/test/java/com/bluepapa32').toPath())
    }


    def "watch one directory"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file('src/main/java').toPath())
        watcher.paths.contains(file('src/main/java/com').toPath())
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
    }

    def "watch more than one directory"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java', 'src/test/java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 6
        watcher.paths.contains(file('src/main/java').toPath())
        watcher.paths.contains(file('src/main/java/com').toPath())
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
        watcher.paths.contains(file('src/test/java').toPath())
        watcher.paths.contains(file('src/test/java/com').toPath())
        watcher.paths.contains(file('src/test/java/com/bluepapa32').toPath())
    }

    def "watch more than one file or directory"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java', 'src/test/java/com/bluepapa32/MainTest.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 4
        watcher.paths.contains(file('src/main/java').toPath())
        watcher.paths.contains(file('src/main/java/com').toPath())
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
        watcher.paths.contains(file('src/test/java/com/bluepapa32').toPath())
    }

     def "watch a file that does not exist"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files files('src/main/java/com/bluepapa32/MainTest.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        thrown(java.nio.file.NoSuchFileException)
    }

    def "watch a directory which has the specified files"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files fileTree(dir: 'src/main/java', include: '**/*.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file('src/main/java').toPath())
        watcher.paths.contains(file('src/main/java/com').toPath())
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
    }

    def "watch a directory which has no specified files"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files fileTree(dir: 'src/main/java', include: '**/*.groovy')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 3
        watcher.paths.contains(file('src/main/java').toPath())
        watcher.paths.contains(file('src/main/java/com').toPath())
        watcher.paths.contains(file('src/main/java/com/bluepapa32').toPath())
    }

    def "watch a directory that does not exist"() {
        given:
        WatchTarget target = new WatchTarget()
        target.files fileTree(dir: 'src/main/groovy', include: '**/*.groovy')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        thrown(java.nio.file.NoSuchFileException)
    }

    def "watch a empty directory"() {

        assert !file('src/hoge').exists()
        mkdir 'src/hoge/java'

        def mkdirs = true

        given:
        WatchTarget target = new WatchTarget()
        target.files fileTree(dir: 'src/hoge/java', include: '**/*.java')

        and:
        DefaultWatcher watcher = new DefaultWatcher()

        when:
        target.register(watcher)

        then:
        watcher.paths.size() == 1
        watcher.paths.contains(file('src/hoge/java').toPath())

        cleanup:
        if (mkdirs) delete 'src/hoge'
    }
}

