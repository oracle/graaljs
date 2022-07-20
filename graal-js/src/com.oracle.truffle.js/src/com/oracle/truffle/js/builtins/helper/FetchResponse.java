package com.oracle.truffle.js.builtins.helper;

import java.net.URL;

public class FetchResponse {
    private URL url;
    private int status;
    private String statusText;
    private int counter;
    private FetchHeaders headers;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public FetchHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(FetchHeaders headers) {
        this.headers = headers;
    }
}
