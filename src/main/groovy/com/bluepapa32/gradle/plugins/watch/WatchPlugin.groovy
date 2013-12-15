package com.bluepapa32.gradle.plugins.watch

import org.gradle.api.Plugin
import org.gradle.api.Project

class WatchPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.extensions.watch = project.container(WatchTarget) { name ->
            project.extensions.create(name, WatchTarget, name)
        }

        project.tasks.addRule("Pattern: watch.<Name>: Run watch task specified by name.") { taskName ->
            if (taskName == 'watchRun') {
                project.task('watchRun') << { logger.lifecycle 'Successfully started.' }
            } else if (project.watch.find { "watch${it.name[0].toUpperCase()}${it.name[1..-1]}" == taskName }) {
                project.task(taskName, type: WatchTask)
            }
        }

        project.task('watch', type: WatchTask) {
            description = 'Runs predefined tasks whenever watched files are added, changed or deleted.'
        }
    }
}
