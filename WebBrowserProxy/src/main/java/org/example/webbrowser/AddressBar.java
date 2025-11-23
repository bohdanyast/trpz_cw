package org.example.webbrowser;

import java.util.regex.Pattern;

public class AddressBar {
    private String url;
    private String protocol;

    public AddressBar() {
        this.url = "";
        this.protocol = "https";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean validateURL(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String urlPattern = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$";
        Pattern pattern = Pattern.compile(urlPattern);

        return pattern.matcher(url).matches();
    }
}