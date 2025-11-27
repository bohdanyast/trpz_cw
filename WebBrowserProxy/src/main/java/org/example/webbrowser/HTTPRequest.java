package org.example.webbrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String url;
    private String method;

    public HTTPRequest(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Sends request to server and gets HTTPResponse
     * Uses WebPageFetcher to download all resources and cache locally
     * Returns file:// URL to index.html so WebEngine can load resources properly
     */
    public HTTPResponse sendRequest() {
        HTTPResponse response = new HTTPResponse();

        try {
            // Generate cache directory based on domain
            String domain = extractDomain(url);
            String cacheDir = "./browser_cache/" + domain;

            // Use WebPageFetcher to download page and all resources
            WebPageFetcher fetcher = new WebPageFetcher(url, cacheDir);
            String indexHtmlPath = fetcher.fetchAndSave();

            // Read the saved HTML file
            String htmlContent = new String(Files.readAllBytes(Paths.get(indexHtmlPath)));

            // Create successful response with file:// URL
            response.setStatusCode(200);
            response.setBody(htmlContent);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "text/html");
            headers.put("X-Cache-Dir", cacheDir);
            headers.put("X-Index-Path", indexHtmlPath);
            headers.put("X-File-URL", new File(indexHtmlPath).toURI().toString());
            headers.put("X-Fetcher", "WebPageFetcher");
            response.setHeaders(headers);

            System.out.println("Page loaded successfully via WebPageFetcher");
            System.out.println("Cache location: " + cacheDir);
            System.out.println("File URL: " + new File(indexHtmlPath).toURI().toString());

        } catch (Exception e) {
            // If WebPageFetcher fails, try simple HTTP request
            System.err.println("WebPageFetcher failed, falling back to simple HTTP request");
            e.printStackTrace();
            response = sendSimpleRequest();
        }

        return response;
    }

    /**
     * Simple HTTP request (fallback method)
     */
    private HTTPResponse sendSimpleRequest() {
        HTTPResponse response = new HTTPResponse();

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int statusCode = connection.getResponseCode();
            response.setStatusCode(statusCode);

            // Read headers
            Map<String, String> headers = new HashMap<>();
            connection.getHeaderFields().forEach((key, value) -> {
                if (key != null) {
                    headers.put(key, String.join(", ", value));
                }
            });
            response.setHeaders(headers);

            // Read response body
            StringBuilder body = new StringBuilder();
            BufferedReader reader;

            if (statusCode >= 200 && statusCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append("\n");
            }
            reader.close();

            response.setBody(body.toString());
            connection.disconnect();

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Extracts domain from URL for cache directory naming
     */
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            // Remove www. prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            // Replace dots with underscores for folder name
            host = host.replace(".", "_");
            return host;
        } catch (Exception e) {
            return "default";
        }
    }
}