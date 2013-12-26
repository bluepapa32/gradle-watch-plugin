package com.bluepapa32.gradle.plugins.watch;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

public interface Watcher extends Closeable {

    void register(Path path) throws IOException;
    WatchKey take() throws InterruptedException;
}
