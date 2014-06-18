package com.bluepapa32.gradle.plugins.watch;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.gradle.api.logging.LogLevel.INFO;

public class WatchTargetTaskRunner implements AutoCloseable {

    private ProjectConnection connection;

    private StartupProgressMonitor monitor;

    public WatchTargetTaskRunner(Project project) {

        monitor = new StartupProgressMonitor(project);
        monitor.run();

        connection = GradleConnector.newConnector()
                .useInstallation(project.getGradle().getGradleHomeDir())
                .forProjectDirectory(project.getProjectDir())
                .connect();

        connection
            .newBuild()
            .withArguments("-q")
            .run(monitor);
    }

    public void run(List<WatchTarget> targets) {

        if (targets == null || targets.isEmpty()) {
            return;
        }

        BuildLauncher launcher = connection.newBuild();

        for (WatchTarget target : targets) {
            launcher.forTasks(target.getTasks().toArray(new String[0]));
        }

        final int[] taskNum = new int[1];

        launcher.addProgressListener(new ProgressListener() {
            public void statusChanged(ProgressEvent event) {
                if ("Execute tasks".equals(event.getDescription())) {
                    taskNum[0]++;
                }
            }
        });

        long timestamp = System.currentTimeMillis();

        try {

            launcher.run();

        } catch (BuildException e) {
            // ignore...
        }

        for (WatchTarget target : targets) {
            target.setExecutedAt(timestamp);
        }
    }

    public void close() {
        monitor.close();
        connection.close();
    }

    private class StartupProgressMonitor implements ResultHandler<Void> {

        private static final int INTERVAL = 500;

        private Project project;

        private PrintStream out = System.out;

        private ScheduledExecutorService timer;

        public StartupProgressMonitor(Project project) {
            this.project = project;
            this.out = System.out;
            this.timer = Executors.newScheduledThreadPool(1);
        }

        public void run() {

            LogLevel logLevel = project.getGradle().getStartParameter().getLogLevel();
            if (INFO.compareTo(logLevel) < 0) {
                System.setOut(new DevNullPrintStream());
            }

            out.print("Starting...");
            out.flush();

            timer.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    out.print("...");
                    out.flush();
                }

            }, INTERVAL, INTERVAL, MILLISECONDS);
        }

        public void onComplete(Void result) {
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    out.println(" OK");
                    System.setOut(out);
                    timer.shutdownNow();
                }
            }, INTERVAL, MILLISECONDS);
        }

        public void onFailure(GradleConnectionException failure) {
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    out.println(" NG");
                    System.setOut(out);
                    timer.shutdownNow();
                }
            }, INTERVAL, MILLISECONDS);
        }

        public void close() {
            if (!timer.isShutdown()) {
                timer.shutdownNow();
            }
        }
    }
}
