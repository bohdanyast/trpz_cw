package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 502 Bad Gateway responses
 */
public class BadGatewayHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 502;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        // Enhance error response
        if (!response.getBody().contains("502")) {
            String errorPage = generateErrorPage();
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "BadGatewayHandler");
        response.getHeaders().put("X-Error-Type", "Server Error");
        response.getHeaders().put("X-Retry-Recommended", "true");
    }
    
    /**
     * Generates a user-friendly 502 error page
     * 
     * @return HTML content for 502 error page
     */
    private String generateErrorPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>502 - Bad Gateway</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #f39c12; }
                </style>
            </head>
            <body>
                <h1>502 - Bad Gateway</h1>
                <p>The server received an invalid response. Please try again later.</p>
            </body>
            </html>
            """;
    }
}
