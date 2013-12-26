package com.bluepapa32.gradle.plugins.watch

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class WatchTargetSpec extends Specification {

    def project = new ProjectBuilder().withProjectDir(new File('src/test/project')).build()

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

