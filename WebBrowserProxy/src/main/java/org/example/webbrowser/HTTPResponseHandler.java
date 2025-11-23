package org.example.webbrowser;

/**
 * Chain of Responsibility Pattern: Handler Interface
 *
 * This interface defines the contract for HTTP response handlers.
 * Each handler in the chain can process a specific HTTP status code
 * or pass the request to the next handler in the chain.
 */
public interface HTTPResponseHandler {

    /**
     * Sets the next handler in the chain
     *
     * @param next The next handler to call if this handler cannot process the response
     */
    void setNext(HTTPResponseHandler next);

    /**
     * Handles the HTTP response
     * Each handler checks if it can process the given status code.
     * If yes, it handles the response and returns true.
     * If no, it passes the response to the next handler in the chain.
     *
     * @param response The HTTP response to handle
     * @return true if the response was handled, false otherwise
     */
    boolean handle(HTTPResponse response);
}
