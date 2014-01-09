package com.bluepapa32.gradle.plugins.watch;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;

public class DefaultWatcher implements Watcher {

    @SuppressWarnings("rawtypes")
    private static final Kind[] EVENT_KIND = {
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWatcher.class);

    private WatchService service;

    private Map<Path, WatchKey> paths;

    private boolean closed;

    public DefaultWatcher() throws IOException {
        this.service = FileSystems.getDefault().newWatchService();
        this.paths   = new LinkedHashMap<>();
    }

    Set<Path> getPaths() {
        return Collections.unmodifiableSet(paths.keySet());
    }

    @Override
    public void register(Path path) throws IOException {

        if (closed) {
            throw new ClosedWatchServiceException();
        }

        if (!Files.exists(path)) {
            throw new java.nio.file.NoSuchFileException(path.toString());
        }

        if (!Files.isDirectory(path)) {

            Path dir = path.getParent();
            paths.put(dir, dir.register(service, EVENT_KIND, HIGH));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is registered.", dir);
            }

            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {

                paths.put(dir, dir.register(service, EVENT_KIND, HIGH));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} is registered.", dir);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void unregister(Path path) throws IOException {

        if (closed) {
            throw new ClosedWatchServiceException();
        }

        Iterator<Entry<Path, WatchKey>> it = paths.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Path, WatchKey> e = it.next();

            Path p = e.getKey();
            if (!p.startsWith(path)) {
                continue;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is unregistered", p);
            }

            e.getValue().cancel();
            it.remove();
        }

        for (Entry<Path, WatchKey> e : paths.entrySet()) {
            if (e.getValue().isValid()) {
                continue;
            }

            Path p = e.getKey();
            paths.put(p, p.register(service, EVENT_KIND, HIGH));
        }
    }

    public boolean isWatching(Path path) {
        return paths.containsKey(path);
    }

    public WatchKey take() throws InterruptedException {

        if (closed) {
            throw new ClosedWatchServiceException();
        }

        return service.take();
    }

    public void close() throws IOException {
        closed = true;
        paths.clear();
        service.close();
    }
}
