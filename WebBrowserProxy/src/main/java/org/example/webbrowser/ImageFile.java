package org.example.webbrowser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageFile implements IImage {
    private String fileName;
    private String filePath;
    private String content; // Base64 encoded content
    private boolean loaded;

    public ImageFile(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = "";
        this.loaded = false;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void loadImage() {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            this.content = Base64.getEncoder().encodeToString(fileContent);
            this.loaded = true;
        } catch (IOException e) {
            this.loaded = false;
        }
    }

    @Override
    public void display() {
        if (!loaded) {
            loadImage();
        }
    }
}