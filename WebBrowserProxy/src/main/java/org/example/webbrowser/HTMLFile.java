package org.example.webbrowser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HTMLFile {
    private String fileName;
    private String filePath;
    private String content;
    
    public HTMLFile(String fileName, String filePath, String content) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = content;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public void loadHTML() {
        StringBuilder contentBuilder = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            this.content = contentBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}