package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 200 OK responses
 */
public class SuccessHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 200;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        response.getHeaders().put("X-Handled-By", "SuccessHandler");
    }
}
