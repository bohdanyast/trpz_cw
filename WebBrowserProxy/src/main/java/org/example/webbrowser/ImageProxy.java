package org.example.webbrowser;

import java.util.Base64;

public class ImageProxy implements IImage {
    private ImageFile realImage; // RealSubject
    private String fileName;
    private String filePath;
    private String placeholderContent; // Заглушка
    private boolean isRealImageLoaded;

    public ImageProxy(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.isRealImageLoaded = false;
        this.placeholderContent = createPlaceholder();
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getContent() {
        if (isRealImageLoaded && realImage != null) {
            return realImage.getContent();
        }
        return placeholderContent;
    }

    @Override
    public boolean isLoaded() {
        return isRealImageLoaded;
    }

    @Override
    public void display() {
        loadImage();
    }

    @Override
    public void loadImage() {
        if (!isRealImageLoaded) {

            realImage = new ImageFile(fileName, filePath);
            realImage.loadImage();

            isRealImageLoaded = true;
        }
    }

    public String createPlaceholder() {
        String svg = String.format("""
            <svg width='300' height='200' xmlns='http://www.w3.org/2000/svg'>
                <defs>
                    <linearGradient id='grad' x1='0%%' y1='0%%' x2='100%%' y2='100%%'>
                        <stop offset='0%%' style='stop-color:#e0e0e0;stop-opacity:1' />
                        <stop offset='100%%' style='stop-color:#f5f5f5;stop-opacity:1' />
                    </linearGradient>
                </defs>
                <rect width='300' height='200' fill='url(#grad)' stroke='#cccccc' stroke-width='2'/>
                <circle cx='150' cy='80' r='25' fill='#999999' opacity='0.5'/>
                <circle cx='150' cy='80' r='15' fill='#cccccc'/>
                <polygon points='140,75 145,85 155,85 160,75' fill='#999999' opacity='0.5'/>
                <text x='150' y='140' text-anchor='middle' font-family='Arial' font-size='14' fill='#666666'>
                    Loading...
                </text>
                <text x='150' y='160' text-anchor='middle' font-family='Arial' font-size='12' fill='#999999'>
                    %s
                </text>
            </svg>
            """, fileName);

        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes());
    }
}