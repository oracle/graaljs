/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.fetch;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;

public class FetchTestServer {
    private final HttpServer server;

    public FetchTestServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        setupHandlers();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void setupHandlers() {
        server.createContext("/inspect", ctx -> {
            String method = ctx.getRequestMethod();
            String url = ctx.getRequestURI().toString();
            String headers = ctx.getRequestHeaders().entrySet().stream().map(e -> String.format("\"%s\":\"%s\"", e.getKey().toLowerCase(), String.join(", ", e.getValue()))).collect(
                            Collectors.joining(","));
            String reqBody = new String(ctx.getRequestBody().readAllBytes());

            // build inspect response body
            String body = String.format("{\"method\":\"%s\",\"url\":\"%s\",\"headers\":{%s},\"body\": \"%s\"}", method, url, headers, reqBody);

            ctx.getResponseHeaders().set("Content-Type", "application/json");
            ctx.sendResponseHeaders(HTTP_OK, body.getBytes().length);
            ctx.getResponseBody().write(body.getBytes());
            ctx.close();
        });

        server.createContext("/200", ctx -> {
            ctx.sendResponseHeaders(HTTP_OK, 0);
            ctx.close();
        });

        server.createContext("/error/400", ctx -> {
            byte[] body = "client error".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "text/plain");
            ctx.sendResponseHeaders(HTTP_BAD_REQUEST, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/error/500", ctx -> {
            byte[] body = "server error".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "text/plain");
            ctx.sendResponseHeaders(HTTP_INTERNAL_ERROR, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/error/json", ctx -> {
            byte[] body = "<html></html>".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "application/json");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/error/404", ctx -> {
            ctx.getResponseHeaders().set("Content-Encoding", "gzip");
            ctx.sendResponseHeaders(HTTP_NOT_FOUND, -1);
            ctx.close();
        });

        server.createContext("/options", ctx -> {
            byte[] body = "hello world".getBytes();
            ctx.getResponseHeaders().set("Allow", "GET, HEAD, OPTIONS");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/plain", ctx -> {
            byte[] body = "text\uD83D\uDCA9".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/plain-utf-16", ctx -> {
            byte[] body = "text\uD83D\uDCA9".getBytes(StandardCharsets.UTF_16);
            ctx.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-16");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/plain-utf-32", ctx -> {
            byte[] body = "text\uD83D\uDCA9".getBytes(Charset.forName("UTF-32"));
            ctx.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-32");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/html", ctx -> {
            byte[] body = "<html></html>".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "text/html");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        server.createContext("/json", ctx -> {
            byte[] body = "{\"name\":\"value\"}".getBytes();
            ctx.getResponseHeaders().set("Content-Type", "application/json");
            ctx.sendResponseHeaders(HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        Set.of(301, 302, 303, 307, 308).forEach(code -> {
            server.createContext("/redirect/" + code, ctx -> {
                String query = ctx.getRequestURI().getQuery();
                String location = (query == null || query.isEmpty()) ? "/inspect" : query;
                ctx.getResponseHeaders().add("Location", location);
                ctx.sendResponseHeaders(code, 0);
                ctx.close();
            });
        });

        server.createContext("/redirect/bad-location", ctx -> {
            ctx.getResponseHeaders().add("Location", "<>");
            ctx.sendResponseHeaders(HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        server.createContext("/redirect/other-host", ctx -> {
            ctx.getResponseHeaders().add("Location", "https://github.com/oracle/graaljs");
            ctx.sendResponseHeaders(HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        server.createContext("/redirect/chain", ctx -> {
            ctx.getResponseHeaders().add("Location", "/redirect/301");
            ctx.sendResponseHeaders(HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        server.createContext("/redirect/no-location", ctx -> {
            ctx.sendResponseHeaders(HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        server.createContext("/redirect/301/invalid-url", ctx -> {
            ctx.getResponseHeaders().add("Location", "//super:invalid:url%/");
            ctx.sendResponseHeaders(HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        server.createContext("/no-status-text", ctx -> {
            ctx.sendResponseHeaders(0, 0);
            ctx.close();
        });

        server.createContext("/no-content", ctx -> {
            ctx.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            ctx.close();
        });

        server.createContext("/not-modified", ctx -> {
            ctx.sendResponseHeaders(HTTP_NOT_MODIFIED, -1);
            ctx.close();
        });

        server.createContext("/cookie", ctx -> {
            ctx.getResponseHeaders().set("Set-Cookie", "a=1, b=2");
            ctx.sendResponseHeaders(HTTP_OK, 0);
            ctx.close();
        });

        server.createContext("/cookie2", ctx -> {
            ctx.getResponseHeaders().add("Set-Cookie", "a=1, b=2");
            ctx.getResponseHeaders().add("Set-Cookie", "c=3, d=4");
            ctx.sendResponseHeaders(HTTP_OK, 0);
            ctx.close();
        });

        server.createContext("/question", ctx -> {
            ctx.sendResponseHeaders(HTTP_OK, 0);
            ctx.close();
        });
    }
}
