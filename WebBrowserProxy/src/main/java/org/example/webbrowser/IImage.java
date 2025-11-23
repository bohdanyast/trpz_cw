package org.example.webbrowser;

public interface IImage {
    void display();

    void loadImage();

    String getFileName();

    String getFilePath();

    String getContent();

    boolean isLoaded();
}