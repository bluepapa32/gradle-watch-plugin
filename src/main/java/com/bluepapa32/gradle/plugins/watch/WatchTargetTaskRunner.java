package com.bluepapa32.gradle.plugins.watch;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnsupportedVersionException;
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException;
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException;

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
        } catch (UnsupportedOperationConfigurationException ex) {
            ex.printStackTrace();   // TODO
        } catch (UnsupportedBuildArgumentException ex) {
            ex.printStackTrace();   // TODO
        } catch (BuildException ex) {
            ex.printStackTrace();   // TODO
        } catch (UnsupportedVersionException ex) {
            ex.printStackTrace();   // TODO
        } catch (GradleConnectionException ex) {
            ex.printStackTrace();   // TODO
        } catch (IllegalStateException ex) {
            ex.printStackTrace();   // TODO
        }

        for (WatchTarget target : targets) {
            target.setExecutedAt(timestamp);
        }
    }

    public void close() {
        connection.close();
    }
}
