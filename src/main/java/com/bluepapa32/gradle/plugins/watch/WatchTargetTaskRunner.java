package com.bluepapa32.gradle.plugins.watch;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

public class WatchTargetTaskRunner implements AutoCloseable {

    private ProjectConnection connection;

    public WatchTargetTaskRunner(Project project) {
        connection = GradleConnector.newConnector()
                .useInstallation(project.getGradle().getGradleHomeDir())
                .forProjectDirectory(project.getProjectDir())
                .connect();

        connection.newBuild().forTasks("watchRun").run();
    }

    public void run(List<WatchTarget> targets) {

        if (targets.isEmpty()) {
            return;
        }

        BuildLauncher launcher = connection.newBuild();

        for (WatchTarget target : targets) {
            launcher.forTasks(target.getTasks());
        }

        long timestamp = System.currentTimeMillis();

        try {
            launcher.run();
        } catch (BuildException ignore) {
            // ignore...
        }

        for (WatchTarget target : targets) {
            target.setExecutedAt(timestamp);
        }
    }

    public void close() {
        connection.close();
    }
}
