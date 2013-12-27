package com.bluepapa32.gradle.plugins.watch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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

            Set<WatchTarget> actualTargets = new HashSet<>();

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

                    runner.run(new ArrayList<>(actualTargets));
                    actualTargets.clear();

                    if (!key.reset()) {
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

            if (actualTargets.add(target)) {
                added = true;
            }
        }

        return added;
    }
}
