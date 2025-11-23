package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 404 Not Found responses
 * 
 * Handles cases where the requested resource does not exist on the server.
 * This is a client error (4xx series).
 */
public class NotFoundHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 404;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        if (!response.getBody().contains("404")) {
            String errorPage = generateErrorPage();
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "NotFoundHandler");
        response.getHeaders().put("X-Error-Type", "Client Error");
    }
    
    /**
     * Generates 404 error page
     * 
     * @return HTML content for 404 error page
     */
    private String generateErrorPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>404 - Not Found</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #e74c3c; }
                </style>
            </head>
            <body>
                <h1>404 - Page Not Found</h1>
                <p>The page you are looking for does not exist.</p>
            </body>
            </html>
            """;
    }
}
