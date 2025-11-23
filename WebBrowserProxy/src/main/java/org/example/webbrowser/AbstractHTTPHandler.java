package org.example.webbrowser;

/**
 * Chain of Responsibility Pattern: Abstract Handler
 * 
 * Base class for all HTTP response handlers.
 * Implements the chain linking logic and provides a template for concrete handlers.
 */
public abstract class AbstractHTTPHandler implements HTTPResponseHandler {
    
    protected HTTPResponseHandler nextHandler;
    
    @Override
    public void setNext(HTTPResponseHandler next) {
        this.nextHandler = next;
    }
    
    @Override
    public boolean handle(HTTPResponse response) {
        if (canHandle(response)) {
            processResponse(response);
            return true;
        } else if (nextHandler != null) {
            return nextHandler.handle(response);
        }
        return false;
    }
    
    /**
     * Determines if this handler can process the given response
     * 
     * @param response The HTTP response
     * @return true if this handler can process the response
     */
    protected abstract boolean canHandle(HTTPResponse response);
    
    /**
     * Processes the HTTP response
     * This method is called only if canHandle() returns true
     * 
     * @param response The HTTP response to process
     */
    protected abstract void processResponse(HTTPResponse response);
}
