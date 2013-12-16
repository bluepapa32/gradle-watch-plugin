package com.bluepapa32.gradle.plugins.watch;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Collections.singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
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
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskAction;

public class WatchTask extends DefaultTask {

    private static final String TASK_NAME = "watch";

    @SuppressWarnings("rawtypes")
    private static final Kind[] EVENT_KIND = {
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
    };

    private Collection<WatchTarget> targets;

    public WatchTask() {
        super();

        ExtensionContainer ext = getProject().getExtensions();

        @SuppressWarnings("unchecked")
        Collection<WatchTarget> targets = (Collection<WatchTarget>) ext.getByName(TASK_NAME);
        if (!getName().startsWith(TASK_NAME)) {
            this.targets = targets;
        } else {
            String name = getName().substring(TASK_NAME.length());
            if (!name.isEmpty()) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
                for (WatchTarget target : targets) {
                    if (target.getName().equals(name)) {
                        this.targets = singleton(target);
                        break;
                    }
                }
            }
        }
    }

    @TaskAction
    public void watch() throws IOException {

        try (WatchService service = FileSystems.getDefault().newWatchService()) {

            for (WatchTarget target : targets) {

                for (File file : target.getFiles()) {

                    Path path = file.toPath();

                    if (!file.isDirectory()) {
                        path.getParent().register(service, EVENT_KIND);

                    } else {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                                dir.register(service, EVENT_KIND);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                }
            }

            List<WatchTarget> actualTargets = new ArrayList<>();
            Set<Path> changedPaths = new LinkedHashSet<>();

            try (WatchTargetTaskRunner runner = new WatchTargetTaskRunner(getProject())) {

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
                            getLogger().lifecycle(format("%2$tF %2$tT File %s changed.%n",
                                path, new Date(path.toFile().lastModified())));
                        }
                    }
                    changedPaths.clear();

                    runner.run(actualTargets);

                    actualTargets.clear();

                    if (!key.reset()) {
                        break;
                    }
                }
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

        FileCollection files = target.getFiles();
        if (files instanceof FileTree) {
            return files.contains(path.toFile());
        }

        for (File file : files) {
            if (path.startsWith(file.toPath())) {
                return true;
            }
        }

        return false;
    }
}
