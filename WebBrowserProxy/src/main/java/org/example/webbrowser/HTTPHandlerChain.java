package org.example.webbrowser;

public class HTTPHandlerChain {
    
    private HTTPResponseHandler firstHandler;

    public HTTPHandlerChain() {
        buildDefaultChain();
    }
    
    /**
     * Builds the default chain of handlers
     */
    private void buildDefaultChain() {
        // Create handlers
        HTTPResponseHandler notFoundHandler = new NotFoundHandler();
        HTTPResponseHandler badGatewayHandler = new BadGatewayHandler();
        HTTPResponseHandler serviceUnavailableHandler = new ServiceUnavailableHandler();
        HTTPResponseHandler successHandler = new SuccessHandler();

        // Link handlers in chain
        successHandler.setNext(notFoundHandler);
        notFoundHandler.setNext(badGatewayHandler);
        badGatewayHandler.setNext(serviceUnavailableHandler);

        // Set first handler
        firstHandler = successHandler;
    }
    
    /**
     * Processes an HTTP response through the handler chain
     * 
     * @param response The HTTP response to process
     * @return true if response was handled by any handler in the chain
     */
    public boolean process(HTTPResponse response) {
        if (firstHandler == null) {
            return false;
        }
        return firstHandler.handle(response);
    }
}
