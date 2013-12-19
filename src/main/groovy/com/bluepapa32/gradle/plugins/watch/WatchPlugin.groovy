package com.bluepapa32.gradle.plugins.watch

import org.gradle.api.Plugin
import org.gradle.api.Project

class WatchPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.extensions.watch = project.container(WatchTarget) { name ->
            project.extensions.create(name, WatchTarget, name)
        }

        project.tasks.addRule("Pattern: watch<Name>: Watch the only target that is specified by name.") { taskName ->

            if (!taskName.startsWith('watch')) {
                return;
            }

            def targets = project.watch.findAll {
                "watch${it.name[0].toUpperCase()}${it.name[1..-1]}" == taskName
            }

            if (targets) {
                project.task(taskName, type: WatchTask) {
                    watch targets
                }
            }
        }

        project.task('watch', type: WatchTask, description: 'Runs predefined tasks whenever watched files are added, changed or deleted.') {
            watch project.watch
        }
    }
}
