package com.bluepapa32.gradle.plugins.watch;

import org.gradle.api.Named;
import org.gradle.api.file.FileCollection;

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

//	---------------------------------------------------------------------------

    private long executedAt;

    void setExecutedAt(long executedAt) {
    	this.executedAt = executedAt;
    }

    long getExecutedAt() {
    	return executedAt;
    }
}
