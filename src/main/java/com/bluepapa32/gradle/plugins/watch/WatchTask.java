package com.bluepapa32.gradle.plugins.watch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class WatchTask extends DefaultTask {

    private Collection<WatchTarget> targets;
    private Path projectPath;

    public WatchTask() {
        projectPath = getProject().getProjectDir().toPath();
    }

    Collection<WatchTarget> getTargets() {
        return targets;
    }

    public void watch(Collection<WatchTarget> targets) {
        this.targets = targets;
    }

    @TaskAction
    public void watch() throws IOException {

        if (targets == null || targets.isEmpty()) {
            return;
        }

        try (Watcher service = new DefaultWatcher()) {

            for (WatchTarget target : targets) {
                target.register(service);
            }

            Set<WatchTarget> actualTargets = new HashSet<>();

            try (WatchTargetTaskRunner runner = new WatchTargetTaskRunner(getProject())) {

                while (true) {

                    WatchKey key;

                    try {
                        key = service.take();
                    } catch (InterruptedException e) {
                        return;
                    }

                    Date timestamp = new Date();
                    Path dir = (Path) key.watchable();

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[TAKE ] WatchKey@{} for \"{}\".",
                                          System.identityHashCode(key),
                                          projectPath.relativize(dir));
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {

                        if (event.kind() == OVERFLOW) {
                            continue;
                        }

                        Path name = (Path) event.context();
                        Path path = dir.resolve(name);

                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("\"{}\" was {}. ({})",
                                              projectPath.relativize(path),
                                              toString(event.kind()),
                                              new Date(path.toFile().lastModified()));
                        }

                        if (Files.isDirectory(path)) {

                            if (event.kind() == ENTRY_CREATE) {

                                getLogger().lifecycle("");
                                getLogger().lifecycle(
                                        "----------------------------------------"
                                        + "----------------------------------------");
                                getLogger().lifecycle(" {}", timestamp);

                                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                    throws IOException {

                                        getLogger().lifecycle(" Directory \"{}\" was created.",
                                                              projectPath.relativize(dir));
                                        return FileVisitResult.CONTINUE;
                                    }
                                });

                                getLogger().lifecycle(
                                        "----------------------------------------"
                                        + "----------------------------------------");

                                service.register(path);
                            }

                            continue;
                        }

                        if (event.kind() == ENTRY_DELETE && service.isWatching(path)) {

                            getLogger().lifecycle("");
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");
                            getLogger().lifecycle(" {}", timestamp);
                            getLogger().lifecycle(" Directory \"{}\" was deleted.",
                                                projectPath.relativize(path));
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");

                            service.unregister(path);
                            continue;
                        }

                        if (addWatchTarget(actualTargets, path)) {
                            getLogger().lifecycle("");
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");
                            getLogger().lifecycle(" {}", timestamp);
                            getLogger().lifecycle(" File \"{}\" was {}.",
                                                  projectPath.relativize(path),
                                                  toString(event.kind()));
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");
                        }
                    }

                    runner.run(new ArrayList<>(actualTargets));
                    actualTargets.clear();

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[RESET] WatchKey@{} for \"{}\".",
                                          System.identityHashCode(key),
                                          projectPath.relativize(dir));
                    }

                    key.reset();

                    if (!key.isValid() && Files.exists(dir)) {
                        getLogger().error("WatchKey@{} for \"{}\" is not valid.",
                                          System.identityHashCode(key),
                                          projectPath.relativize(dir));
                        break;
                    }
                }
            }
        }
    }

    private boolean addWatchTarget(Collection<WatchTarget> actualTargets, Path path) {

        boolean added = false;

        for (WatchTarget target : targets) {

            if (!target.isTarget(path)) {
                continue;
            }

            actualTargets.add(target);
            added = true;
        }

        return added;
    }

    private String toString(WatchEvent.Kind<?> eventKind) {

        if (eventKind == ENTRY_CREATE) {
            return "created";
        }

        if (eventKind == ENTRY_MODIFY) {
            return "changed";
        }

        if (eventKind == ENTRY_DELETE) {
            return "deleted";
        }

        throw new IllegalStateException(String.valueOf(eventKind));
    }
}
