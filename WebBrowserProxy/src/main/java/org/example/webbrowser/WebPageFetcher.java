package org.example.webbrowser;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Web page fetcher that downloads HTML and all its resources (CSS, JS, images)
 */
public class WebPageFetcher {
    private String baseUrl;
    private String outputDir;
    private Set<String> downloadedResources;
    private String indexHtmlPath;
    private String protocol;
    private String host;

    public WebPageFetcher(String url, String outputDir) {
        this.baseUrl = url;
        this.outputDir = outputDir;
        this.downloadedResources = new HashSet<>();

        try {
            URL urlObj = new URL(url);
            this.protocol = urlObj.getProtocol();
            this.host = urlObj.getHost();
        } catch (Exception e) {
            this.protocol = "https";
            this.host = "";
        }
    }

    /**
     * Fetches page and all resources, returns path to index.html
     */
    public String fetchAndSave() throws IOException {
        // Create output directory
        Files.createDirectories(Paths.get(outputDir));

        // Check if page is already cached
        Path cachedHtml = Paths.get(outputDir, "index.html");
        if (Files.exists(cachedHtml)) {
            System.out.println("Loading from cache: " + outputDir);
            indexHtmlPath = cachedHtml.toAbsolutePath().toString();
            return indexHtmlPath;
        }

        System.out.println("Fetching page: " + baseUrl);

        // Fetch main HTML
        String html = fetchResource(baseUrl);

        if (html == null || html.trim().isEmpty()) {
            throw new IOException("Failed to fetch HTML content");
        }

        System.out.println("HTML fetched, length: " + html.length());

        // Download resources in order of importance

        // 1. CSS files (multiple patterns)
        html = downloadAndReplaceResources(html,
                "<link[^>]*href=[\"']([^\"']+\\.css[^\"']*)[\"'][^>]*>",
                "css", "link");
        html = downloadAndReplaceResources(html,
                "@import\\s+url\\([\"']?([^\"')]+\\.css[^\"')]*)[\"']?\\)",
                "css", "import");

        // 2. JavaScript files (multiple patterns)
        html = downloadAndReplaceResources(html,
                "<script[^>]*src=[\"']([^\"']+\\.js[^\"']*)[\"'][^>]*>",
                "js", "script");

        // 3. Images (multiple formats and patterns)
        html = downloadAndReplaceResources(html,
                "<img[^>]*src=[\"']([^\"']+\\.(jpg|jpeg|png|gif|svg|webp|ico|bmp)[^\"']*)[\"'][^>]*>",
                "images", "img");

        // 4. Background images in style attributes
        html = downloadAndReplaceResources(html,
                "url\\([\"']?([^\"')]+\\.(jpg|jpeg|png|gif|svg|webp)[^\"')]*)[\"']?\\)",
                "images", "bg");

        // 5. Link rel icons (favicon, apple-touch-icon, etc)
        html = downloadAndReplaceResources(html,
                "<link[^>]*rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>",
                "images", "icon");

        // 6. Fonts
        html = downloadAndReplaceResources(html,
                "url\\([\"']?([^\"')]+\\.(woff2?|ttf|eot|otf)[^\"')]*)[\"']?\\)",
                "fonts", "font");

        // Add base tag to HTML for proper relative URL resolution
        html = addBaseTag(html);

        // Save modified HTML
        Files.write(cachedHtml, html.getBytes());

        indexHtmlPath = cachedHtml.toAbsolutePath().toString();

        System.out.println("\n=== Download Summary ===");
        System.out.println("Total resources downloaded: " + downloadedResources.size());
        System.out.println("Index.html saved at: " + indexHtmlPath);

        return indexHtmlPath;
    }

    /**
     * Adds base tag to HTML for proper relative URL resolution
     */
    private String addBaseTag(String html) {
        // Check if base tag already exists
        if (html.contains("<base")) {
            return html;
        }

        // Add base tag after <head>
        String baseTag = "<base href=\"" + protocol + "://" + host + "/\">";
        html = html.replaceFirst("(<head[^>]*>)", "$1\n" + baseTag);

        return html;
    }

    private String downloadAndReplaceResources(String html, String regex, String folder, String type) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        StringBuffer result = new StringBuffer();

        int count = 0;

        while (matcher.find()) {
            String resourceUrl = matcher.group(1);

            // Skip data URLs, empty URLs, blob URLs, and already local paths
            if (resourceUrl.startsWith("data:") ||
                    resourceUrl.startsWith("blob:") ||
                    resourceUrl.trim().isEmpty() ||
                    resourceUrl.startsWith("file://") ||
                    resourceUrl.startsWith("#")) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            // Resolve full URL
            String fullUrl = resolveUrl(baseUrl, resourceUrl);

            // Skip if already downloaded
            if (downloadedResources.contains(fullUrl)) {
                // Find local path for this URL
                String localPath = getLocalPathForUrl(fullUrl, folder);
                if (localPath != null) {
                    matcher.appendReplacement(result,
                            Matcher.quoteReplacement(matcher.group(0).replace(resourceUrl, localPath)));
                } else {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                }
                continue;
            }

            try {
                String localPath = downloadResource(fullUrl, folder);
                downloadedResources.add(fullUrl);
                count++;

                // Replace in HTML
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement(matcher.group(0).replace(resourceUrl, localPath)));

                System.out.println("[" + type + "] Downloaded (" + count + "): " + getFilenameFromUrl(fullUrl));

            } catch (IOException e) {
                System.err.println("[" + type + "] Failed: " + fullUrl + " - " + e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        if (count > 0) {
            System.out.println("[" + type + "] Total downloaded: " + count);
        }

        return result.toString();
    }

    private Map<String, String> urlToLocalPathMap = new HashMap<>();

    private String downloadResource(String url, String folder) throws IOException {
        byte[] data = fetchBinaryResource(url);

        // Create folder if not exists
        Path folderPath = Paths.get(outputDir, folder);
        Files.createDirectories(folderPath);

        // Generate filename
        String filename = getFilenameFromUrl(url);
        Path filePath = folderPath.resolve(filename);

        // Handle duplicate filenames
        int counter = 1;
        while (Files.exists(filePath)) {
            String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
            String ext = filename.substring(filename.lastIndexOf('.'));
            filename = nameWithoutExt + "_" + counter + ext;
            filePath = folderPath.resolve(filename);
            counter++;
        }

        // Save file
        Files.write(filePath, data);

        String localPath = folder + "/" + filename;
        urlToLocalPathMap.put(url, localPath);

        return localPath;
    }

    private String getLocalPathForUrl(String url, String folder) {
        return urlToLocalPathMap.get(url);
    }

    private String fetchResource(String url) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();

            // Handle redirects manually if needed
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    System.out.println("Following redirect to: " + newUrl);
                    return fetchResource(newUrl);
                }
            }

            if (responseCode != 200) {
                throw new IOException("HTTP response code: " + responseCode);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private byte[] fetchBinaryResource(String url) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();

            // Handle redirects
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    return fetchBinaryResource(newUrl);
                }
            }

            if (responseCode != 200) {
                throw new IOException("HTTP response code: " + responseCode);
            }

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            // Remove whitespace
            relativeUrl = relativeUrl.trim();

            // If already absolute URL
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl;
            }

            // If protocol-relative URL (//example.com/...)
            if (relativeUrl.startsWith("//")) {
                return protocol + ":" + relativeUrl;
            }

            // Use URI to resolve relative URLs
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(relativeUrl);
            return resolved.toString();

        } catch (URISyntaxException e) {
            System.err.println("Failed to resolve URL: " + relativeUrl);
            return relativeUrl;
        }
    }

    private String getFilenameFromUrl(String url) {
        try {
            // Remove query parameters and anchors
            String path = url.split("\\?")[0];
            path = path.split("#")[0];

            // Get last part of path
            String[] parts = path.split("/");
            String filename = parts[parts.length - 1];

            // If no filename or no extension, generate one
            if (filename.isEmpty() || !filename.contains(".")) {
                filename = "resource_" + Math.abs(url.hashCode());

                // Try to detect file type from URL
                if (url.contains(".css")) filename += ".css";
                else if (url.contains(".js")) filename += ".js";
                else if (url.contains(".png")) filename += ".png";
                else if (url.contains(".jpg") || url.contains(".jpeg")) filename += ".jpg";
                else if (url.contains(".gif")) filename += ".gif";
                else if (url.contains(".svg")) filename += ".svg";
                else if (url.contains(".woff2")) filename += ".woff2";
                else if (url.contains(".woff")) filename += ".woff";
                else filename += ".bin";
            }

            // Sanitize filename - remove invalid characters
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

            // Limit filename length
            if (filename.length() > 200) {
                String ext = "";
                int dotIndex = filename.lastIndexOf('.');
                if (dotIndex > 0) {
                    ext = filename.substring(dotIndex);
                    filename = filename.substring(0, 200 - ext.length()) + ext;
                } else {
                    filename = filename.substring(0, 200);
                }
            }

            return filename;

        } catch (Exception e) {
            return "resource_" + Math.abs(url.hashCode()) + ".bin";
        }
    }
}