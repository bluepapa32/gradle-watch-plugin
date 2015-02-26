package com.bluepapa32.gradle.plugins.watch;

import java.io.PrintStream;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.logging.LogLevel;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import static org.gradle.api.logging.LogLevel.INFO;

public class WatchTargetTaskRunner implements AutoCloseable {

    private static final Logger LOG = Logging.getLogger(WatchTargetTaskRunner.class);

    private String[] arguments;

    private ProjectConnection connection;

    public WatchTargetTaskRunner(Project project) {

        StartParameter parameter = project.getGradle().getStartParameter();

        this.arguments = getArguments(parameter);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Arguments: {}", Arrays.toString(arguments));
        }

        final PrintStream out = System.out;

        LogLevel logLevel = parameter.getLogLevel();
        if (INFO.compareTo(logLevel) < 0) {
            System.setOut(new DevNullPrintStream());
        }

        out.print("Starting");
        out.flush();

        connection = GradleConnector.newConnector()
                .useInstallation(project.getGradle().getGradleHomeDir())
                .forProjectDirectory(project.getProjectDir())
                .connect();

        connection
            .newBuild()
            .withArguments("-q")
            .addProgressListener(new ProgressListener() {
                public void statusChanged(ProgressEvent event) {
                    out.print("..");
                    out.flush();
                }
            })
            .run(new ResultHandler<Void>() {
                public void onComplete(Void result) {
                    System.setOut(out);
                    out.println(" OK");
                }
                public void onFailure(GradleConnectionException failure) {
                }
            });
    }

    public void run(List<WatchTarget> targets) {

        if (targets == null || targets.isEmpty()) {
            return;
        }

        BuildLauncher launcher = connection
                                    .newBuild()
                                    .withArguments(arguments)
                                    .setStandardOutput(System.out);

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
        connection.close();
    }

    private String[] getArguments(StartParameter parameter) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> e : parameter.getProjectProperties().entrySet()) {
            args.add("-P" + e.getKey() + "=" + e.getValue());
        }
        return args.toArray(new String[args.size()]);
    }

}
