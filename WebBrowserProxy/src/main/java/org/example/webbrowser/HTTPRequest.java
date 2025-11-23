package org.example.webbrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String url;
    private String method;
    
    public HTTPRequest(String url, String method) {
        this.url = url;
        this.method = method;
    }
    
    public String getUrl() {
        return url;
    }

    public HTTPResponse sendRequest() {
        HTTPResponse response = new HTTPResponse();
        
        try {
            // Setting URL and connection timeout to 5 seconds
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int statusCode = connection.getResponseCode();
            response.setStatusCode(statusCode);

            // Forming headers
            Map<String, String> headers = new HashMap<>();
            connection.getHeaderFields().forEach((key, value) -> {
                if (key != null) {
                    headers.put(key, String.join(", ", value));
                }
            });
            response.setHeaders(headers);
            
            StringBuilder body = new StringBuilder();
            BufferedReader reader;
            
            if (statusCode >= 200 && statusCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append("\n");
            }
            reader.close();
            
            response.setBody(body.toString());
            connection.disconnect();
            
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
}