package org.example.webbrowser;

import java.util.HashMap;
import java.util.Map;

public class HTTPResponse {
    private Integer statusCode;
    private Map<String, String> headers;
    private String body;
    
    public HTTPResponse() {
        this.statusCode = 0;
        this.headers = new HashMap<>();
        this.body = "";
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
}