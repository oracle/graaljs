package com.oracle.truffle.js.builtins.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

public class FetchHttpConnection {
    // https://fetch.spec.whatwg.org/#redirect-status
    private static final List<Integer> REDIRECT_STATUS = Arrays.asList(HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER, 307, 308);

    public static FetchResponse open(FetchRequest request) throws Exception {
        HttpURLConnection connection;
        FetchHeaders headers = new FetchHeaders();
        URL s = null;
        do {
            connection = (HttpURLConnection) request.getUrl().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(request.getMethod().toString());

            int status = connection.getResponseCode();

            if (isRedirect(status)) {
                String location = connection.getHeaderField("Location");
                URL locationURL = new URL(location);

                request.incrementRedirectCount();
                request.setUrl(locationURL);

            }
        } while(connection.getResponseCode() != HTTP_OK);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            body.append(line);
            body.append('\n');
        }
        in.close();

        FetchResponse response = new FetchResponse();
        response.setUrl(request.getUrl());
        response.setCounter(request.getRedirectCount());
        response.setStatusText(connection.getResponseMessage());
        response.setStatus(connection.getResponseCode());
        response.setHeaders(new FetchHeaders(connection.getHeaderFields()));

        return response;
    }

    private static boolean isRedirect(int status) {
        return REDIRECT_STATUS.contains(status);
    }
}
