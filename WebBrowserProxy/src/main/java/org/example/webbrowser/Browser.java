package org.example.webbrowser;

/**
 * For now Browser loads resources but prints them with system.out.println
 * In future this will and should be changed
 */
public class Browser {
    private AddressBar addressBar;
    private WebPage currentPage;
    
    public Browser() {
        this.addressBar = new AddressBar();
    }
    
    public void setAddressBar(AddressBar addressBar) {
        this.addressBar = addressBar;
    }
    
    public AddressBar getAddressBar() {
        return addressBar;
    }
    
    public WebPage getCurrentPage() {
        return currentPage;
    }

    public void loadPage(String url) {
        if (addressBar.validateURL(url)) {
            HTTPRequest request = new HTTPRequest(url, "GET");
            HTTPResponse response = request.sendRequest();
            
            if (response.getStatusCode() == 200) {
                currentPage = new WebPage();
                currentPage.parseHTML();
                currentPage.loadResources();
            } else {
                handleError(response.getStatusCode());
            }
        } else {
            handleError(400);
        }
    }

    public void handleError(Integer errorCode) {
        switch (errorCode) {
            case 400:
                System.err.println("Error 400: Bad Request - Invalid URL");
                break;
            case 404:
                System.err.println("Error 404: Page Not Found");
                break;
            case 500:
                System.err.println("Error 500: Internal Server Error");
                break;
            case 502:
                System.err.println("Error 502: Bad Gateway");
                break;
            case 503:
                System.err.println("Error 503: Service Unavailable");
            default:
                System.err.println("Error " + errorCode + ": An error occurred");
        }
    }
}