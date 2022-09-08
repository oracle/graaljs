/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.*;

/**
 * Implementation of HTTP Fetch as of https://fetch.spec.whatwg.org/commit-snapshots/9bb2ded94073377ec5d9b5e3cda391df6c769a0a/.
 */
public class FetchHttpConnection {
    public static final Set<String> SUPPORTED_SCHEMA = Set.of("data", "http", "https");
    // https://fetch.spec.whatwg.org/#redirect-status
    public static final Set<Integer> REDIRECT_STATUS = Set.of(HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER, 307, 308);
    // https://w3c.github.io/webappsec-referrer-policy/#referrer-policy
    public static final Set<String> VALID_POLICY = Set.of("",
            "no-referrer",
            "no-referrer-when-downgrade",
            "same-origin",
            "origin",
            "strict-origin",
            "origin-when-cross-origin",
            "strict-origin-when-cross-origin",
            "unsafe-url");
    public static final String DEFAULT_REFERRER_POLICY = "strict-origin-when-cross-origin";
    public static Node node = null;

    public static FetchResponse connect(FetchRequest request) throws IOException {
        // to overwrite Host header (https://stackoverflow.com/a/8172736/10708558)
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        if (node == null) {
            throw new IllegalStateException("originating node is null");
        }

        if (!SUPPORTED_SCHEMA.contains(request.getUrl().getProtocol())) {
            throw Errors.createTypeError("fetch cannot load " +
                    request.getUrl().toString() +
                    ". Scheme not supported: " +
                    request.getUrl().getProtocol()
            );
        }

        // Setup Connection
        HttpURLConnection connection = (HttpURLConnection) request.getUrl().openConnection();
        connection.setInstanceFollowRedirects(false);       // don't follow automatically
        connection.setRequestMethod(request.getMethod());
        connection.setDoOutput(true);

        // Par. 4.1, Main fetch step 7
        if (request.getReferrerPolicy().isEmpty()) {
            request.setReferrerPolicy(DEFAULT_REFERRER_POLICY);
        }

        // Setup Headers
        setRequestHeaders(connection, request);

        // Setup Requests Body
        if (request.body != null) {
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(request.body);
            out.flush();
            out.close();
        }

        // Establish connection to the resource
        // connection can now be treated as `actualResponse` as in Par. 4.3 https://fetch.spec.whatwg.org/#http-fetch
        connection.connect();

        int status = connection.getResponseCode();

        // Par. 4.4, HTTP-redirect fetch steps https://fetch.spec.whatwg.org/#http-redirect-fetch
        if (isRedirect(status)) {
            URL locationURL = null;
            String location = connection.getHeaderField("Location");

            try {
                // HTTP-redirect fetch step 3
                if (location != null) {
                    locationURL = URI.create(request.getUrl().toString()).resolve(location).toURL();
                }
            } catch (IllegalArgumentException exception) {
                if (!request.getRedirectMode().equals("manual")) {
                    throw Errors.createFetchError("invalid url in location header", "unsupported-redirect", node);
                }
            }

            // Par. 4.3 Http fetch step 8
            switch (request.getRedirectMode()) {
                case "manual":
                    // return response as is
                    break;
                case "error":
                    throw Errors.createFetchError("uri requested responds with a redirect, redirect mode is set to error", "no-redirect", node);
                case "follow":
                    // HTTP-redirect fetch step 4
                    if (locationURL == null) {
                        break;
                    }

                    // HTTP-redirect fetch step 7
                    if (request.getRedirectCount() >= request.getFollow()) {
                        throw Errors.createFetchError("maximum redirect reached at: " + request.getUrl(), "max-redirect", node);
                    }

                    // HTTP-redirect fetch step 8
                    request.incrementRedirectCount();

                    // remove sensitive headers if redirecting to a new domain or protocol changes
                    if (!isDomainOrSubDomain(locationURL, request.getUrl()) || !isSameProtocol(locationURL, request.getUrl())) {
                        Set.of("authorization", "www-authenticate", "cookie", "cookie2").forEach(k -> {
                            request.headers.delete(k);
                        });
                    }

                    // HTTP-redirect fetch step 11
                    if (status != 303 && request.body != null) {
                        throw Errors.createFetchError("Cannot follow redirect with body", "unsupported-redirect", node);
                    }

                    // HTTP-redirect fetch step 12
                    if (status == HTTP_SEE_OTHER || ((status == HTTP_MOVED_PERM || status == HTTP_MOVED_TEMP) && request.getMethod().equals("POST"))) {
                        request.setMethod("GET");
                        request.setBody(null);
                        request.headers.delete("content-length");
                        request.headers.delete("content-type");
                        request.headers.delete("content-location");
                        request.headers.delete("content-language");
                        request.headers.delete("content-encoding");
                    }

                    // HTTP-redirect fetch step 17
                    request.setUrl(locationURL);

                    // HTTP-redirect fetch step 18
                    // https://w3c.github.io/webappsec-referrer-policy/#set-requests-referrer-policy-on-redirect
                    List<String> policyTokens = connection.getHeaderFields().get("referrer-policy");
                    if (policyTokens != null) {
                        policyTokens.stream().filter(VALID_POLICY::contains).reduce((a, b) -> b).ifPresent(request::setReferrerPolicy);
                    }

                    // HTTP-redirect fetch step 19 invoke fetch, following the redirect
                    return connect(request);
                default:
                    throw Errors.createTypeError("Redirect option " + request.getRedirectMode() + " is not a valid value of RequestRedirect");
            }
        }

        // Prepare Response
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }

        BufferedReader br = null;
        if (inputStream != null) {
            br = new BufferedReader(new InputStreamReader(inputStream));
        }
        String responseBody = br != null ? br.lines().collect(Collectors.joining()) : "";

        FetchResponse response = new FetchResponse();
        response.setBody(responseBody);
        response.setUrl(request.getUrl());
        response.setCounter(request.getRedirectCount());
        response.setStatusText(connection.getResponseMessage());
        response.setStatus(connection.getResponseCode());
        response.setHeaders(new FetchHeaders(connection.getHeaderFields()));

        return response;
    }

    public static boolean isRedirect(int status) {
        return REDIRECT_STATUS.contains(status);
    }

    public static boolean isDomainOrSubDomain(URL destination, URL origin) {
        String d = destination.getHost();
        String o = origin.getHost();
        return d.equals(o) || o.endsWith("." + d);
    }

    public static boolean isSameProtocol(URL destination, URL origin) {
        return destination.getProtocol().equals(origin.getProtocol());
    }

    private static void setRequestHeaders(HttpURLConnection connection, FetchRequest req) {
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate,br");
        if (req.body != null) {
            int length = req.getBodyBytes();
            connection.setFixedLengthStreamingMode(length);
        } else if (Set.of("POST", "PUT").contains(req.getMethod())) {
            connection.setFixedLengthStreamingMode(0);
        }
        connection.setRequestProperty("User-Agent", "graaljs-fetch");

        // Par. 4.6. HTTP-network-or-cache fetch step 11
        if (req.isReferrerUrl()) {
            connection.setRequestProperty("Referer", req.getReferrer());
        }

        // user specified headers
        req.headers.keys().forEach(key -> {
            connection.setRequestProperty(key, req.headers.get(key));
        });
    }
}
