Gradle Watch Plugin 0.1.0
=========================

Run predefined tasks whenever watched file patterns are added, changed or deleted.


Requirements
------------

- Oracle JDK7+
- Gradle 1.9+


Usage
-----

To use the Watch plugin, include in your build script:

Example 1. Using the Watch plugin

build.gradle
~~~
buildscript {
    repositories { maven { url 'http://bluepapa32.github.io/maven/' } } 
    dependencies { classpath 'com.bluepapa32.gradle.plugins:gradle-watch-plugin:0.1.0' }
}   

apply plugin: 'watch'
~~~


Tasks
-----

The Watch plugin adds the following tasks to the project.

Table 1. Watch plugin - tasks

|Task name|Depends on|Type     |Description                                                                       |
|:-------:|:--------:|:-------:|----------------------------------------------------------------------------------|
|watch    |-         |WatchTask|Run predefined tasks whenever watched file patterns are added, changed or deleted.|


Configuration
-------------
~~~
watch {

    java {
        files files('src/main/java')
        tasks 'compileJava'
    }

    resources {
        files fileTree(dir: 'src/main/resources',
                       include: '**/*.properties')
        tasks 'processResources'
    }

    hoge {
        files files('foo/bar', 'foo/boo/hoo.txt')
        tasks 'hogehoge', 'hugohugo'
    }
}
~~~

