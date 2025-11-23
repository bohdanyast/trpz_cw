package org.example.webbrowser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPage {
    private List<HTMLFile> htmlResources;
    private List<CSSFile> cssResources;
    private List<JSFile> jsResources;
    private List<IImage> imageResources;
    private String rawHTML;

    public WebPage() {
        this.htmlResources = new ArrayList<>();
        this.cssResources = new ArrayList<>();
        this.jsResources = new ArrayList<>();
        this.imageResources = new ArrayList<>();
    }

    public List<HTMLFile> getHtmlResources() {
        return htmlResources;
    }

    public List<CSSFile> getCssResources() {
        return cssResources;
    }

    public List<JSFile> getJsResources() {
        return jsResources;
    }

    public List<IImage> getImageResources() {
        return imageResources;
    }

    public void setRawHTML(String rawHTML) {
        this.rawHTML = rawHTML;
    }

    public String getRawHTML() {
        return rawHTML;
    }

    public void parseHTML() {
        if (rawHTML == null || rawHTML.isEmpty()) {
            return;
        }

        Pattern cssPattern = Pattern.compile("<link[^>]*href=[\"']([^\"']*\\.css)[\"'][^>]*>");
        Matcher cssMatcher = cssPattern.matcher(rawHTML);
        while (cssMatcher.find()) {
            String cssPath = cssMatcher.group(1);
            CSSFile cssFile = new CSSFile(extractFileName(cssPath), cssPath);
            cssResources.add(cssFile);
        }

        Pattern jsPattern = Pattern.compile("<script[^>]*src=[\"']([^\"']*\\.js)[\"'][^>]*>");
        Matcher jsMatcher = jsPattern.matcher(rawHTML);
        while (jsMatcher.find()) {
            String jsPath = jsMatcher.group(1);
            JSFile jsFile = new JSFile(extractFileName(jsPath), jsPath);
            jsResources.add(jsFile);
        }

        Pattern imgPattern = Pattern.compile("<img[^>]*src=[\"']([^\"']*)[\"'][^>]*>");
        Matcher imgMatcher = imgPattern.matcher(rawHTML);
        while (imgMatcher.find()) {
            String imgPath = imgMatcher.group(1);
            ImageFile imageFile = new ImageFile(extractFileName(imgPath), imgPath);
            imageResources.add(imageFile);
        }

        HTMLFile mainHTML = new HTMLFile("index.html", "index.html", rawHTML);
        htmlResources.add(mainHTML);
    }


    public void loadResources() {
        for (CSSFile css : cssResources) {
            css.loadCSS();
        }

        for (JSFile js : jsResources) {
            js.loadJS();
        }

        for (IImage img : imageResources) {
            img.loadImage();
        }
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}