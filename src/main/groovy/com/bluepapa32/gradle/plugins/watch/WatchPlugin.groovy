package com.bluepapa32.gradle.plugins.watch

import org.gradle.api.Plugin
import org.gradle.api.Project

class WatchPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.extensions.watch = project.container(WatchTarget) { name ->
            project.extensions.create(name, WatchTarget, name)
        }

        project.task('watchRun') << {
            println 'Successfully started watcher.'
        }

        project.task('watch', type: WatchTask) {
            description = 'Run predefined tasks whenever watched files are added, changed or deleted.'
        }
    }
}
