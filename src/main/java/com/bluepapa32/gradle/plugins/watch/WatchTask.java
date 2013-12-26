package com.bluepapa32.gradle.plugins.watch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class WatchTask extends DefaultTask {

    private Collection<WatchTarget> targets;

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

        Path projectPath = getProject().getProjectDir().toPath();

        try (Watcher service = new DefaultWatcher()) {

            for (WatchTarget target : targets) {
                target.register(service);
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
                            getLogger().lifecycle("");
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");
                            getLogger().lifecycle(" \033[36m{}\033[39m",
                                                         new Date(path.toFile().lastModified()));
                            getLogger().lifecycle(" File \"{}\" changed.",
                                                         projectPath.relativize(path));
                            getLogger().lifecycle(
                                    "----------------------------------------"
                                    + "----------------------------------------");
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

        for (WatchTarget target : targets) {

            if (!target.isTarget(path)) {
                continue;
            }

            added = true;
            actualTargets.add(target);
        }

        return added;
    }
}
