package com.bluepapa32.gradle.plugins.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.DefaultFileTreeElement;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import static org.gradle.util.CollectionUtils.toStringList;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;


public class WatchTarget implements Named {

    @SuppressWarnings("rawtypes")
    private static final Kind[] EVENT_KIND = {
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
    };

    private String name;
    private List<FileCollection> fileCollections = new ArrayList<>();
    private String[] tasks;

    public WatchTarget(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String[] getTasks() {
        return tasks;
    }

    public void files(FileCollection files) {
        fileCollections.add(files);
    }

    public void tasks(String... tasks) {
        this.tasks = tasks;
    }

//  ------------------------------------------------------------- package private

    private long executedAt;

    void setExecutedAt(long executedAt) {
        this.executedAt = executedAt;
    }

    void register(final WatchService service) throws IOException {

        for (FileCollection files : fileCollections) {

            for (File file : files) {

                Path path = file.toPath();

                if (!file.isDirectory()) {
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
        }
    }

    boolean isTarget(Path path) {

        File f = path.toFile();

        long lastModified = f.lastModified();
        if (0 < lastModified && lastModified <= executedAt) {
            return false;
        }

        for (FileCollection fileCollection : fileCollections) {

            if (fileCollection instanceof DirectoryTree) {

                DirectoryTree dirTree = (DirectoryTree) fileCollection;

                Path dir = dirTree.getDir().toPath();
                if (!path.startsWith(dir)) {
                    return false;
                }

                String[] segments = toStringList(dir.relativize(path)).toArray(new String[0]);

                return dirTree.getPatterns().getAsSpec().isSatisfiedBy(
                        new DefaultFileTreeElement(f, new RelativePath(true, segments)));
            }

            if (fileCollection instanceof FileTree) {
                return fileCollection.contains(f);
            }

            for (File file : fileCollection) {
                if (path.startsWith(file.toPath())) {
                    return true;
                }
            }
        }

        return false;
    }
}
