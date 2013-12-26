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

    public DefaultWatcher() throws IOException {
        this.service = FileSystems.getDefault().newWatchService();
    }

    @Override
    public void register(Path path) throws IOException {

        if (!Files.exists(path)) {
            throw new java.nio.file.NoSuchFileException(path.toString());
        }

        if (!Files.isDirectory(path)) {
            path.getParent().register(service, EVENT_KIND, HIGH);
        } else {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
                    dir.register(service, EVENT_KIND, HIGH);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public WatchKey take() throws InterruptedException {
        return service.take();
    }

    public void close() throws IOException {
        service.close();
    }
}
