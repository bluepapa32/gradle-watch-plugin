package com.bluepapa32.gradle.plugins.watch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;

public class DefaultWatcher implements Watcher {

    @SuppressWarnings("rawtypes")
    private static final Kind[] EVENT_KIND = {
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
    };

    private WatchService service;

    private Set<Path> paths;

    public DefaultWatcher() throws IOException {
        this.service = FileSystems.getDefault().newWatchService();
        this.paths   = new HashSet<>();
    }

    Set<Path> getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    @Override
    public void register(Path path) throws IOException {

        if (!Files.exists(path)) {
            throw new java.nio.file.NoSuchFileException(path.toString());
        }

        if (!Files.isDirectory(path)) {
            Path dir = path.getParent();
            dir.register(service, EVENT_KIND, HIGH);
            paths.add(dir);
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
                dir.register(service, EVENT_KIND, HIGH);
                paths.add(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public WatchKey take() throws InterruptedException {
        return service.take();
    }

    public void close() throws IOException {
        service.close();
    }
}
