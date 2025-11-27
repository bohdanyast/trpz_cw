package org.example.webbrowser;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.ResourceBundle;

public class WebBrowserController implements Initializable {
    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    private WebEngine webEngine;
    private WebHistory webHistory;

    private Browser browser;
    private AddressBar addressBar;
    private WebPage currentWebPage;

    // Local web server for testing
    private WebServer localServer;

    // Chain of Responsibility for HTTP response handling
    private HTTPHandlerChain handlerChain;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();

        // Initialize new architecture
        browser = new Browser();
        addressBar = new AddressBar();
        browser.setAddressBar(addressBar);

        // Initialize Chain of Responsibility for HTTP handling
        handlerChain = new HTTPHandlerChain();

        // Initialize local test web server
        initializeLocalServer();

        // Listener for page loading
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
                injectLinkHandler();
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Page loading failed");
                browser.handleError(500);
            }
        });

        // Listen for location changes (navigation)
        webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation != null && !newLocation.isEmpty()) {
                // Update text field with current URL
                textField.setText(extractOriginalUrl(newLocation));
            }
        });

        loadPage();
    }

    /**
     * Injects JavaScript to handle link clicks
     */
    private void injectLinkHandler() {
        try {
            // Make this controller accessible from JavaScript
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaController", this);

            // Inject JavaScript to handle link clicks
            String script = """
                (function() {
                    // Remove old listeners if any
                    if (window.linkHandlerInstalled) return;
                    window.linkHandlerInstalled = true;
                    
                    // Add click listener to document
                    document.addEventListener('click', function(e) {
                        // Find closest <a> tag
                        var target = e.target;
                        while (target && target.tagName !== 'A') {
                            target = target.parentElement;
                        }
                        
                        if (target && target.tagName === 'A') {
                            var href = target.getAttribute('href');
                            
                            // Skip if no href or special links
                            if (!href || 
                                href.startsWith('#') || 
                                href.startsWith('javascript:') ||
                                href.startsWith('mailto:') ||
                                href.startsWith('tel:')) {
                                return;
                            }
                            
                            // Check if target="_blank" or ctrl/cmd key pressed
                            if (target.getAttribute('target') === '_blank' || 
                                e.ctrlKey || e.metaKey) {
                                // Let browser handle it normally
                                return;
                            }
                            
                            // Prevent default navigation
                            e.preventDefault();
                            e.stopPropagation();
                            
                            // Call Java method to handle navigation
                            console.log('Navigating to: ' + href);
                            window.javaController.handleLinkClick(href);
                        }
                    }, true);
                    
                    console.log('Link handler installed successfully');
                })();
                """;

            webEngine.executeScript(script);
            System.out.println("Link handler injected successfully");

        } catch (Exception e) {
            System.err.println("Failed to inject link handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called from JavaScript when user clicks a link
     * This method is exposed to JavaScript via JSObject
     */
    public void handleLinkClick(String href) {
        System.out.println("\n=== Link Clicked ===");
        System.out.println("Link: " + href);

        try {
            // Get current page URL
            String currentUrl = webEngine.getLocation();

            // Resolve relative URL to absolute
            String absoluteUrl = resolveUrl(currentUrl, href);
            System.out.println("Resolved to: " + absoluteUrl);

            // Update text field
            textField.setText(extractOriginalUrl(absoluteUrl));

            // Navigate to new page
            navigateToUrl(absoluteUrl);

        } catch (Exception e) {
            System.err.println("Error handling link click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Resolves relative URL to absolute based on current page
     */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            // If already absolute
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl;
            }

            // If protocol-relative
            if (relativeUrl.startsWith("//")) {
                java.net.URL base = new java.net.URL(baseUrl);
                return base.getProtocol() + ":" + relativeUrl;
            }

            // If file:// URL (local cache)
            if (baseUrl.startsWith("file://")) {
                // Extract original URL from our cache structure
                // browser_cache/example_com/ -> https://example.com
                String originalUrl = extractOriginalUrl(baseUrl);
                java.net.URL base = new java.net.URL(originalUrl);
                java.net.URL resolved = new java.net.URL(base, relativeUrl);
                return resolved.toString();
            }

            // Normal relative URL
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            return resolved.toString();

        } catch (Exception e) {
            System.err.println("Failed to resolve URL: " + relativeUrl);
            return relativeUrl;
        }
    }

    /**
     * Extracts original URL from file:// path
     * Example: file:///C:/browser_cache/example_com/index.html -> https://example.com
     */
    private String extractOriginalUrl(String fileUrl) {
        if (!fileUrl.startsWith("file://")) {
            return fileUrl;
        }

        try {
            // Extract domain from cache path
            // browser_cache/example_com/ -> example.com
            String path = fileUrl;
            int cacheIndex = path.indexOf("browser_cache");
            if (cacheIndex != -1) {
                String afterCache = path.substring(cacheIndex + "browser_cache".length() + 1);
                String domain = afterCache.split("/")[0];

                // Convert example_com back to example.com
                domain = domain.replace("_", ".");

                return "https://" + domain;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract original URL from: " + fileUrl);
        }

        return fileUrl;
    }

    /**
     * Navigates to URL (handles both test.com and real websites)
     */
    private void navigateToUrl(String url) {
        // Update address bar
        addressBar.setUrl(url);

        // Check if test.com
        if (url.contains("test.com")) {
            handleLocalServerRequest(url);
        } else {
            loadRealWebsite(url);
        }
    }

    /**
     * Initializes local test web server
     */
    private void initializeLocalServer() {
        localServer = new WebServer("test.com");

        // Add test resources
        localServer.addResource("index.html");

        // Create test page for successful response (200)
        WebPage testPage = new WebPage();
        String testHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Server - Success</title>
                <style>
                    body { 
                        font-family: Arial; 
                        padding: 50px; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                        color: white; 
                    }
                    h1 { font-size: 48px; }
                    p { font-size: 20px; }
                    a { color: #FFD700; text-decoration: underline; }
                    a:hover { color: #FFA500; }
                </style>
            </head>
            <body>
                <h1>Test Server Page</h1>
                <p>This page is served by the local WebServer!</p>
                <p>Status: 200 OK</p>
                <p>Handler: SuccessHandler (Chain of Responsibility)</p>
                <hr>
                <h2>Test Links:</h2>
                <img src="img1.jpg"></img>
                <p><a href="test.com/404">Test 404 Error</a></p>
                <p><a href="test.com/502">Test 502 Error</a></p>
                <p><a href="test.com/503">Test 503 Error</a></p>
                <p><a href="https://example.com">Go to Example.com</a></p>
            </body>
            </html>
            """;
        testPage.setRawHTML(testHTML);
        testPage.parseHTML();
        localServer.addPage(testPage);

        System.out.println("Local WebServer initialized at: " + localServer.getHost());
        System.out.println("Test URLs:");
        System.out.println("  - test.com/index.html (200 OK)");
        System.out.println("  - test.com/404 (404 Not Found)");
        System.out.println("  - test.com/502 (502 Bad Gateway)");
        System.out.println("  - test.com/503 (503 Service Unavailable)");
        System.out.println("\nReal websites: Enter any URL (e.g., example.com)");
    }

    public void loadPage() {
        String inputUrl = textField.getText();

        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            return;
        }

        // Check if this is a request to local test server
        if (inputUrl.contains("test.com")) {
            handleLocalServerRequest(inputUrl);
            return;
        }

        // Use AddressBar for validation
        if (addressBar.validateURL(inputUrl)) {
            // Form full URL with protocol
            String fullUrl = inputUrl;
            if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                fullUrl = addressBar.getProtocol() + "://" + inputUrl;
            }

            addressBar.setUrl(fullUrl);

            loadRealWebsite(fullUrl);

        } else {
            browser.handleError(400);
            System.err.println("Invalid URL: " + inputUrl);
        }
    }

    /**
     * Loads real website using WebPageFetcher
     * Downloads all resources and loads via file:// URL
     */
    private void loadRealWebsite(String url) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("LOADING WEBSITE: " + url);
        System.out.println("=".repeat(50));

        try {
            // Create HTTP request
            HTTPRequest request = new HTTPRequest(url, "GET");

            // Send request - WebPageFetcher will download everything
            HTTPResponse response = request.sendRequest();

            // Process response through Chain of Responsibility
            handlerChain.process(response);

            // Load page via file:// URL
            String fileUrl = response.getHeaders().get("X-File-URL");
            if (fileUrl != null) {
                webEngine.load(fileUrl);
            } else {
                webEngine.loadContent(response.getBody(), "text/html");
            }

            // Parse page for our architecture
            currentWebPage = new WebPage();
            currentWebPage.setRawHTML(response.getBody());
            currentWebPage.parseHTML();

        } catch (Exception e) {
            browser.handleError(500);
        }
    }

    /**
     * Handles requests to local test server
     */
    private void handleLocalServerRequest(String url) {
        String fullUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            fullUrl = "http://" + url;
        }

        HTTPRequest request = new HTTPRequest(fullUrl, "GET");
        HTTPResponse response;

        if (url.contains("/404")) {
            response = new HTTPResponse();
            response.setStatusCode(404);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");

        } else if (url.contains("/502")) {
            response = new HTTPResponse();
            response.setStatusCode(502);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");

        } else if (url.contains("/503")) {
            response = new HTTPResponse();
            response.setStatusCode(503);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");
            response.getHeaders().put("Retry-After", "30");

        } else {
            response = localServer.processRequest(request);
        }

        // Chain of Responsibility processes response
        handlerChain.process(response);

        String displayContent = response.getBody();
        if (response.getStatusCode() == 200) {
            displayContent = replaceImagesWithProxies(displayContent);
        }

        webEngine.loadContent(displayContent, "text/html");

        currentWebPage = new WebPage();
        currentWebPage.setRawHTML(response.getBody());
        currentWebPage.parseHTML();
    }

    /**
     * Replaces all <img> tags with proxy placeholder images
     *
     * @param html Original HTML content
     * @return HTML with images replaced by proxy placeholders
     */
    private String replaceImagesWithProxies(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // Pattern to match <img> tags
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
                "<img([^>]*?)src=[\"']([^\"']*)[\"']([^>]*?)>",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = imgPattern.matcher(html);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String beforeSrc = matcher.group(1);
            String originalSrc = matcher.group(2);
            String afterSrc = matcher.group(3);

            // Extract filename from path
            String fileName = originalSrc;
            int lastSlash = originalSrc.lastIndexOf('/');
            if (lastSlash >= 0) {
                fileName = originalSrc.substring(lastSlash + 1);
            }

            // Create ImageProxy and get placeholder
            ImageProxy proxy = new ImageProxy(fileName, originalSrc);
            String placeholderDataURI = proxy.createPlaceholder();

            // Replace with proxy placeholder
            String replacement = "<img" + beforeSrc +
                    "src=\"" + placeholderDataURI + "\" " +
                    "data-original-src=\"" + originalSrc + "\"" +
                    afterSrc + ">";

            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Called when page successfully loaded
     */
    private void onPageLoaded() {
        try {
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            if (currentWebPage == null) {
                currentWebPage = new WebPage();
            }

            currentWebPage.setRawHTML(htmlContent);
            currentWebPage.parseHTML();

        } catch (Exception e) {
            System.err.println("Error in onPageLoaded: " + e.getMessage());
        }
    }

    public void reload() {
        String currentUrl = addressBar.getUrl();
        if (currentUrl != null && !currentUrl.isEmpty()) {
            textField.setText(currentUrl);
            loadPage();
        } else {
            webEngine.reload();
        }
    }

    public Browser getBrowser() {
        return browser;
    }
}