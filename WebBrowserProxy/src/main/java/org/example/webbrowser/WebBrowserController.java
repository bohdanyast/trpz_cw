package org.example.webbrowser;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class WebBrowserController implements Initializable {
    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    private WebEngine webEngine;

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

        handlerChain = new HTTPHandlerChain();

        // Initialize local test web server
        initializeLocalServer();

        // Listener for page loading
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
            } else if (newState == Worker.State.FAILED) {
                browser.handleError(500);
            }
        });

        loadPage();
    }

    /**
     * Initializes local test web server
     */
    private void initializeLocalServer() {
        localServer = new WebServer("test.com");

        localServer.addResource("index.html");

        WebPage testPage = new WebPage();
        String testHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Server - Success</title>
                <link rel="stylesheet" href="styles/main.css">
            </head>
            <body>
                <h1>Test Server Page</h1>
                <p>This page is served by the local WebServer!</p>
                <p>Status: 200 OK</p>
                <ul>
                    <li>test.com/404</li>
                    <li>test.com/502</li>
                    <li>test.com/503</li>
                </ul>
                <img src="images/test.png" alt="Test">
                <script src="js/app.js"></script>
            </body>
            </html>
            """;
        testPage.setRawHTML(testHTML);
        testPage.parseHTML();
        localServer.addPage(testPage);

    }

    public void loadPage() {
        String inputUrl = textField.getText();

        // Check if this is a request to local test server
        if (inputUrl.contains("test.com")) {
            handleLocalServerRequest(inputUrl);
            return;
        }

        // Use AddressBar for validation
        if (addressBar.validateURL(inputUrl)) {
            String fullUrl = inputUrl;
            if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                fullUrl = addressBar.getProtocol() + "://" + inputUrl;
            }

            // Update addressBar
            addressBar.setUrl(fullUrl);
            webEngine.load(fullUrl);
            browser.loadPage(fullUrl);

        } else {
            browser.handleError(400);
            System.err.println("Invalid URL: " + inputUrl);
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

        // Create HTTP request (client)
        HTTPRequest request = new HTTPRequest(fullUrl, "GET");
        System.out.println("Client sending request to: " + fullUrl);

        HTTPResponse response;

        // Determine status code based on URL path
        if (url.contains("/404")) {
            // Create 404 response - handler will generate error page
            response = new HTTPResponse();
            response.setStatusCode(404);
            response.setBody(""); // Empty body - handler will fill it
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
            response.getHeaders().put("Retry-After", "30"); // Suggest retry in 30 seconds

        } else {
            response = localServer.processRequest(request);
        }

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
     * This demonstrates Proxy pattern in action
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
            // Get HTML content of loaded page
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            // Create WebPage and parse HTML
            currentWebPage = new WebPage();
            currentWebPage.setRawHTML(htmlContent);
            currentWebPage.parseHTML();

        } catch (Exception e) {
            System.err.println("Error parsing page: " + e.getMessage());
        }
    }

    public void reload() {
        webEngine.reload();
    }
}