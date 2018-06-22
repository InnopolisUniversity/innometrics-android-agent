package com.example.innometrics.server;

/**
 * Class to work with requests from server
 */
public class ServerRequestItem {
    private String url;
    private String token;
    private String body;

    public ServerRequestItem(String url, String token, String body) {
        this.url = url;
        this.token = token;
        this.body = body;
    }

    public void setUrl(String url) {
        this.url = url;

    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUrl() {

        return url;
    }

    public String getToken() {
        return token;
    }

    public String getBody() {
        return body;
    }

    public ServerRequestItem(String url) {
        this.url = url;
    }
}
