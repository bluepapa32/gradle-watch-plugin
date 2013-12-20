package com.bluepapa32.gradle.plugins.watch;

import java.io.File;
import java.nio.file.Path;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.DefaultFileTreeElement;

import static org.gradle.util.CollectionUtils.toStringList;


public class WatchTarget implements Named {

    private String name;
    private FileCollection files;
    private String[] tasks;

    public WatchTarget(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public FileCollection getFiles() {
        return files;
    }

    public String[] getTasks() {
        return tasks;
    }

    public void files(FileCollection files) {
        this.files = files;
    }

    public void tasks(String... tasks) {
        this.tasks = tasks;
    }

//  ------------------------------------------------------------- package private

    private long executedAt;

    void setExecutedAt(long executedAt) {
        this.executedAt = executedAt;
    }

    long getExecutedAt() {
        return executedAt;
    }

    boolean isTarget(Path path) {

        if (files instanceof DirectoryTree) {

            DirectoryTree dirTree = (DirectoryTree) files;

            Path dir = dirTree.getDir().toPath();
            if (!path.startsWith(dir)) {
                return false;
            }

            String[] segments = toStringList(dir.relativize(path)).toArray(new String[0]);

            return dirTree.getPatterns().getAsSpec().isSatisfiedBy(
                    new DefaultFileTreeElement(path.toFile(), new RelativePath(true, segments)));

        }

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
