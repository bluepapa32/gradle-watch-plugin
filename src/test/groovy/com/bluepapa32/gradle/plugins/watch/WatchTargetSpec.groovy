package com.bluepapa32.gradle.plugins.watch

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class WatchTargetSpec extends Specification {

    def project = new ProjectBuilder().withProjectDir(new File('.')).build()

    def "The target files are specified by FileTree"() {
        when:
        WatchTarget target = new WatchTarget();
        target.files fileTree(dir: 'src/main/java', include: '**/*.java')

        then:
        expected == target.isTarget(new File(projectDir, path).toPath());

        where:
        path                                                                 | expected
        "hoge"                                                               | false
        "src"                                                                | false
        "src/main"                                                           | false
        "src/main/java"                                                      | false
        "src/main/java/Main.java"                                            | true
        "src/main/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | true
        "src/main/java/Main.groovy"                                          | false

    }

    def "a directory are specified by FileCollection"() {
        when:
        WatchTarget target = new WatchTarget();
        target.files files('src/main/java')

        then:
        expected == target.isTarget(new File(projectDir, path).toPath());

        where:
        path                                                                 | expected
        "hoge"                                                               | false
        "src"                                                                | false
        "src/main"                                                           | false
        "src/main/java"                                                      | true
        "src/main/java/Main.java"                                            | true
        "src/main/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | true
        "src/main/java/Main.groovy"                                          | true
    }

    def "a file are specified by FileCollection"() {
        when:
        WatchTarget target = new WatchTarget();
        target.files files('src/main/java/Main.java')

        then:
        expected == target.isTarget(new File(projectDir, path).toPath());

        where:
        path                                                                 | expected
        "hoge"                                                               | false
        "src"                                                                | false
        "src/main"                                                           | false
        "src/main/java"                                                      | false
        "src/main/java/Main.java"                                            | true
        "src/main/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | false
        "src/main/java/Main.groovy"                                          | false
    }

    def "same files are specified by FileCollection"() {
        when:
        WatchTarget target = new WatchTarget();
        target.files files('src/main/java/Main.java', 'src/main/java/Hoge.java', 'src/test/java')

        then:
        expected == target.isTarget(new File(projectDir, path).toPath());

        where:
        path                                                                 | expected
        "hoge"                                                               | false
        "src"                                                                | false
        "src/main"                                                           | false
        "src/main/java"                                                      | false
        "src/main/java/Main.java"                                            | true
        "src/main/java/Hoge.java"                                            | true
        "src/main/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | false
        "src/main/java/Main.groovy"                                          | false
        "src/test/java/Main.java"                                            | true
        "src/test/java/Hoge.java"                                            | true
        "src/test/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | true
        "src/test/java/Main.groovy"                                          | true
    }

    def "same files are specified by FileCollection and FileTree"() {
        when:
        WatchTarget target = new WatchTarget();
        target.files files('src/main/java/Main.java', 'src/main/java/Hoge.java')
        target.files fileTree(dir: 'src/test/java', include: '**/*.java');

        then:
        expected == target.isTarget(new File(projectDir, path).toPath());

        where:
        path                                                                 | expected
        "hoge"                                                               | false
        "src"                                                                | false
        "src/main"                                                           | false
        "src/main/java"                                                      | false
        "src/main/java/Main.java"                                            | true
        "src/main/java/Hoge.java"                                            | true
        "src/main/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | false
        "src/main/java/Main.groovy"                                          | false
        "src/test/java/Main.java"                                            | true
        "src/test/java/Hoge.java"                                            | true
        "src/test/java/com/bluepapa32/gradle/plugins/watch/WatchTarget.java" | true
        "src/test/java/Main.groovy"                                          | false
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

