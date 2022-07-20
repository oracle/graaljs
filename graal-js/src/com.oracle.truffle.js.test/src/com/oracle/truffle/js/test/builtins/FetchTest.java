package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.test.JSTest;
//import com.sun.net.httpserver.HttpServer;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FetchTest extends JSTest {
    //private HttpServer testServer;

    @Before
    public void testSetup() throws IOException {
        /*HttpServer testServer = HttpServer.create(new InetSocketAddress(8080), 0);

        testServer.createContext("/echo", ctx -> {
            byte[] body = ctx.getRequestBody().readAllBytes();
            ctx.sendResponseHeaders(HttpURLConnection.HTTP_OK, body.length);
            ctx.getResponseBody().write(body);
            ctx.close();
        });

        testServer.createContext("/ok", ctx -> {
            ctx.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            ctx.close();
        });
        
        testServer.createContext("/redirect/301", ctx -> {
            ctx.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_PERM, 0);
            ctx.close();
        });

        testServer.start();*/
    }

    @After
    public void testCleanup() {
        //testServer.stop(0);
    }

    @Test
    public void testOk() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result = context.eval(ID, "var x = fetch('http://localhost:8080/ok'); x.status === 200 && x.ok;");
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testAllowGETRequest() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/echo', { method: 'GET' })");
        }
    }

    @Test
    public void testAllowPOSTRequest() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/echo', { method: 'POST' })");
        }
    }

    @Test
    public void testAllowPUTRequest() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/echo', { method: 'PUT' })");
        }
    }

    @Test
    public void testAllowHEADRequest() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/echo', { method: 'HEAD' })");
        }
    }

    @Test
    public void testAllowOPTIONSRequest() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/echo', { method: 'OPTIONS' })");
        }
    }

    @Test
    public void testFollowRedirectCodes() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object result = context.eval(ID, "fetch('http://localhost:8080/redirect/301')");
            result = context.eval(ID, "fetch('http://localhost:8080/redirect/302')");
            result = context.eval(ID, "fetch('http://localhost:8080/redirect/303')");
            result = context.eval(ID, "fetch('http://localhost:8080/redirect/304')");
            result = context.eval(ID, "fetch('http://localhost:8080/redirect/307')");
            result = context.eval(ID, "fetch('http://localhost:8080/redirect/308')");
        }
    }


}
