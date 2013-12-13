package com.bluepapa32.gradle.plugins.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnsupportedVersionException;
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException;
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class WatchTask extends DefaultTask {

    private Collection<WatchTarget> targets;

    public WatchTask() throws IOException {
    	super();

        ExtensionContainer ext = getProject().getExtensions();

        @SuppressWarnings("unchecked")
		Collection<WatchTarget> targets = (Collection<WatchTarget>) ext.getByName("watch");
		this.targets = targets;
    }

    @TaskAction
    public void watch() throws IOException {

        try (WatchService service = FileSystems.getDefault().newWatchService()) {

            for (WatchTarget target : targets) {

                for (File file : target.getFiles()) {

                    Path path = file.toPath();

                    if (!file.isDirectory()) {
                        path.getParent().register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

                    } else {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                }
            }

            ProjectConnection connection = GradleConnector.newConnector()
                    .useInstallation(getProject().getGradle().getGradleHomeDir())
                    .forProjectDirectory(getProject().getProjectDir())
                    .connect();

            List<WatchTarget> actualTargets = new ArrayList<>();
            Set<Path> changedPaths = new LinkedHashSet<>();

            try {

        	    connection.newBuild().forTasks("watchRun").run();

                while (true) {

                    WatchKey key;

                    try {
                        key = service.take();
                    } catch (InterruptedException e) {
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {

                        if (event.kind() == OVERFLOW) {
                            continue;
                        }

                        Path dir  = (Path) key.watchable();
                        Path name = (Path) event.context();
                        Path path = dir.resolve(name);

                        if (Files.isDirectory(path)) {
                            continue;
                        }

                        changedPaths.add(path);
                    }

                    for (Path path : changedPaths) {
                        if (addWatchTarget(actualTargets, path)) {
                            System.out.printf("%2$tF %2$tT File %s changed.%n",
                                path, new Date(path.toFile().lastModified()));
                        }
                    }
                    changedPaths.clear();

                    if (!actualTargets.isEmpty()) {

                	    BuildLauncher launcher = connection.newBuild();

                        for (WatchTarget target : actualTargets) {
                    	    launcher.forTasks(target.getTasks());
                        }

                        long timestamp = System.currentTimeMillis();

                        try {
                            launcher.run();
                        } catch (UnsupportedOperationConfigurationException ex) {
                            ex.printStackTrace();
                        } catch (UnsupportedBuildArgumentException ex) {
                            ex.printStackTrace();
                        } catch (BuildException ex) {
                            ex.printStackTrace();
                        } catch (UnsupportedVersionException ex) {
                            ex.printStackTrace();
                        } catch (GradleConnectionException ex) {
                            ex.printStackTrace();
                        } catch (IllegalStateException ex) {
                            ex.printStackTrace();
                        }

                        for (WatchTarget target : actualTargets) {
                    	    target.setExecutedAt(timestamp);
                        }
                    }
                    actualTargets.clear();

                    if (!key.reset()) {
                        break;
                    }
                }

            } finally {
                connection.close();
            }
        }
    }

    private boolean addWatchTarget(List<WatchTarget> actualTargets, Path path) {

        boolean added = false;

        long lastModified = path.toFile().lastModified();

        for (WatchTarget target : targets) {

            if (lastModified <= target.getExecutedAt()) {
                continue;
            }

            if (!isTarget(target, path)) {
                continue;
            }

            added = true;
            actualTargets.add(target);
        }

        return added;
    }

    private boolean isTarget(WatchTarget target, Path path) {

        for (File file : target.getFiles()) {
            if (path.startsWith(file.toPath())) {
                return true;
            }
        }

        return false;
    }
}

