package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 503 Service Unavailable responses
 */
public class ServiceUnavailableHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 503;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        // Check for Retry-After header
        String retryAfter = response.getHeaders().get("Retry-After");
        
        // Enhance error response
        if (!response.getBody().contains("503")) {
            String errorPage = generateErrorPage(retryAfter);
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "ServiceUnavailableHandler");
        response.getHeaders().put("X-Error-Type", "Server Error");
        response.getHeaders().put("X-Retry-Recommended", "true");
    }
    
    /**
     * Generates a user-friendly 503 error page
     * 
     * @param retryAfter Suggested retry time (can be null)
     * @return HTML content for 503 error page
     */
    private String generateErrorPage(String retryAfter) {
        String retryMessage = retryAfter != null 
            ? "<p>Please try again in " + retryAfter + " seconds.</p>"
            : "<p>Please try again in a few moments.</p>";
            
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>503 - Service Unavailable</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #e67e22; }
                </style>
            </head>
            <body>
                <h1>503 - Service Unavailable</h1>
                <p>The server is temporarily unavailable.</p>
                """ + retryMessage + """
            </body>
            </html>
            """;
    }
}
