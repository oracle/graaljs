/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins.fetch;

import com.oracle.truffle.js.builtins.helper.FetchHttpConnection;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.interop.AsyncInteropTest.TestOutput;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.*;

/**
 * Tests for the fetch builtin.
 */
public class FetchMethodTest extends JSTest {
    private static FetchTestServer localServer;

    @BeforeClass
    public static void testSetup() throws IOException {
        localServer = new FetchTestServer(8080);
        localServer.start();
    }

    @AfterClass
    public static void testCleanup() {
        localServer.stop();
    }

    @Test
    public void testReturnsPromise() {
        String out = async(
            "const res = fetch('http://localhost:8080/200');" +
            "console.log(res instanceof Promise);"
        );
        assertEquals("true\n", out);
    }

    @Test
    public void test200() {
        String out = async(
            "const res = await fetch('http://localhost:8080/200');" +
            "console.log(res.ok, res.status, res.statusText);"
        );
        assertEquals("true 200 OK\n", out);
    }

    @Test
    public void testHandleClientErrorResponse() {
        String out = async(
            "const res = await fetch('http://localhost:8080/error/400');" +
            log("res.headers.get('content-type')") +
            log("res.status") +
            log("res.statusText") +
            "const result = await res.text();" +
            log("result")
        );
        assertEquals("text/plain\n400\nBad Request\nclient error\n", out);
    }

    @Test
    public void testHandleServerErrorResponse() {
        String out = async(
            "const res = await fetch('http://localhost:8080/error/500');" +
            log("res.headers.get('content-type')") +
            log("res.status") +
            log("res.statusText") +
            "const result = await res.text();" +
            log("result")
        );
        assertEquals("text/plain\n500\nInternal Server Error\nserver error\n", out);
    }

    @Test
    public void testResolvesIntoResponseObject() {
        String out = async(
            "const res = await fetch('http://localhost:8080/200');" +
            log("res instanceof Response") +
            log("res.headers instanceof Headers") +
            log("res.status") +
            log("res.ok") +
            log("res.statusText")
        );
        assertEquals("true\ntrue\n200\ntrue\nOK\n", out);
    }

    @Test
    public void testRejectUnsupportedProtocol() {
        String out = asyncThrows(
            "const res = await fetch('ftp://example.com/');"
        );
        assertEquals("fetch cannot load ftp://example.com/. Scheme not supported: ftp\n", out);
    }

    @Test(timeout = 5000)
    public void testRejectOnNetworkFailure() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:50000');"
        );
        assertEquals("Connection refused\n", out);
    }

    @Test
    public void testAcceptPlainTextResponse() {
        String out = async(
            "const res = await fetch('http://localhost:8080/plain');" +
            "const result = await res.text();" +
            log("res.headers.get('content-type')") +
            log("res.bodyUsed") +
            log("result")
        );
        assertEquals("text/plain\ntrue\ntext\n", out);
    }

    @Test
    public void testAcceptHtmlResponse() {
        String out = async(
            "const res = await fetch('http://localhost:8080/html');" +
            "const result = await res.text();" +
            log("res.headers.get('content-type')") +
            log("res.bodyUsed") +
            log("result")
        );
        assertEquals("text/html\ntrue\n<html></html>\n", out);
    }

    @Test
    public void testAcceptJsonResponse() {
        String out = async(
            "const res = await fetch('http://localhost:8080/json');" +
            "const result = await res.json();" +
            log("res.headers.get('content-type')") +
            log("res.bodyUsed") +
            log("result.name")
        );
        assertEquals("application/json\ntrue\nvalue\n", out);
    }

    @Test
    public void testSendRequestWithCustomHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', {" +
            "   headers: { 'x-custom-header': 'abc' }" +
            "});" +
            "const result = await res.json();" +
            log("result.headers['x-custom-header']")
        );
        assertEquals("abc\n", out);
    }

    @Test
    public void testCustomHostHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', {" +
            "   headers: { 'host': 'example.com' }" +
            "});" +
            "const result = await res.json();" +
            log("result.headers.host")
        );
        assertEquals("example.com\n", out);
    }

    @Test
    @Ignore("HttpURLConnection does not support custom methods")
    public void testCustomMethod() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', {method: 'foo'});" +
            "const result = await res.json();" +
            log("result.method === 'foo'")
        );
        assertEquals("true\n", out);
    }

    @Test
    public void testFollowRedirectCode301() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testFollowRedirectCode302() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/302');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testFollowRedirectCode303() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/303');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testFollowRedirectCode307() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/307');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testFollowRedirectCode308() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/308');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testRedirectChain() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/chain');" +
            log("res.url, res.status")
        );
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test
    public void testFollowPOSTRequestRedirectWithGET() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "method: 'POST'" +
            "});" +
            "const result = await res.json();" +
            log("res.url, res.status") +
            log("result.method, result.body === ''")
        );
        assertEquals("http://localhost:8080/inspect 200\nGET true\n", out);
    }

    @Test
    @Ignore("HttpURLConnection does not support PATCH")
    public void testFollowPATCHRequestRedirectWithPATCH() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "method: 'PATCH'" +
            "});" +
            "const result = await res.json();" +
            log("res.url, res.status") +
            log("result.method, result.body === ''")
        );
        assertEquals("http://localhost:8080/inspect 200\nPATCH true\n", out);
    }

    @Test
    public void testFollow303WithGET() throws IOException {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/303', {" +
            "method: 'PUT'" +
            "});" +
            "const result = await res.json();" +
            log("res.url, res.status") +
            log("result.method, result.body === ''")
        );
        assertEquals("http://localhost:8080/inspect 200\nGET true\n", out);
    }

    @Test
    public void testMaximumFollowsReached() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/redirect/chain', {" +
            "follow: 1" +
            "});"
        );
        assertEquals("maximum redirect reached at: http://localhost:8080/redirect/chain\n", out);
    }

    @Test
    public void testNoRedirectsAllowed() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "follow: 0" +
            "});"
        );
        assertEquals("maximum redirect reached at: http://localhost:8080/redirect/301\n", out);
    }

    @Test
    public void testRedirectModeManual() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "redirect: 'manual'" +
            "});" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/301\n301\n/inspect\n", out);
    }

    @Test
    public void testRedirectModeManualBrokenLocationHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/bad-location', {" +
            "redirect: 'manual'" +
            "});" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/bad-location\n301\n<>\n", out);
    }

    @Test
    public void testRedirectModeManualOtherHost() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/other-host', {" +
            "redirect: 'manual'" +
            "});" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/other-host\n301\nhttps://github.com/oracle/graaljs\n", out);
    }

    @Test
    public void testRedirectModeManualNoRedirect() {
        String out = async(
            "const res = await fetch('http://localhost:8080/200', {" +
            "redirect: 'manual'" +
            "});" +
            log("res.url") +
            log("res.status") +
            log("res.headers.has('location')")
        );
        assertEquals("http://localhost:8080/200\n200\nfalse\n", out);
    }

    @Test
    public void testRedirectModeError() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "redirect: 'error'" +
            "});"
        );
        assertEquals("uri requested responds with a redirect, redirect mode is set to error\n", out);
    }

    @Test
    public void testFollowRedirectAndKeepHeaders() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "headers: {'x-custom-header': 'abc'}" +
            "});" +
            "const result = await res.json();" +
            log("res.url") +
            log("result.headers['x-custom-header']")
        );
        assertEquals("http://localhost:8080/inspect\nabc\n", out);
    }

    @Test(timeout = 5000)
    public void testDontForwardSensitiveToDifferentHost() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/other-domain', {" +
            "headers: {" +
                "'authorization': 'gets=removed'," +
                "'cookie': 'gets=removed'," +
                "'cookie2': 'gets=removed'," +
                "'www-authenticate': 'gets=removed'," +
                "'safe-header': 'gets=forwarded'" +
                "}" +
            "});" +
            "const headers = new Headers((await res.json()).headers);" +
            log("headers.has('authorization')") +
            log("headers.has('cookie')") +
            log("headers.has('cookie2')") +
            log("headers.has('www-authenticate')") +
            log("headers.get('safe-header')")
        );
        assertEquals("false\nfalse\nfalse\nfalse\ngets=forwarded\n", out);
    }

    @Test
    public void testForwardSensitiveHeadersToSameHost() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301', {" +
            "headers: {" +
                "'authorization': 'gets=forwarded'," +
                "'cookie': 'gets=forwarded'," +
                "'cookie2': 'gets=forwarded'," +
                "'www-authenticate': 'gets=forwarded'," +
                "'safe-header': 'gets=forwarded'" +
            "}" +
            "});" +
            "const headers = new Headers((await res.json()).headers);" +
            log("headers.get('authorization')") +
            log("headers.get('cookie')") +
            log("headers.get('cookie2')") +
            log("headers.get('www-authenticate')") +
            log("headers.get('safe-header')")
        );
        assertEquals("gets=forwarded\ngets=forwarded\ngets=forwarded\ngets=forwarded\ngets=forwarded\n", out);
    }

    @Test
    public void testIsDomainOrSubDomain() throws MalformedURLException {
        // forward headers to same (sub)domain
        assertTrue(FetchHttpConnection.isDomainOrSubDomain(new URL("http://a.com"), new URL("http://a.com")));
        assertTrue(FetchHttpConnection.isDomainOrSubDomain(new URL("http://a.com"), new URL("http://www.a.com")));
        assertTrue(FetchHttpConnection.isDomainOrSubDomain(new URL("http://a.com"), new URL("http://foo.bar.a.com")));
        // dont forward to parent domain, another sibling or a unrelated domain
        assertFalse(FetchHttpConnection.isDomainOrSubDomain(new URL("http://b.com"), new URL("http://a.com")));
        assertFalse(FetchHttpConnection.isDomainOrSubDomain(new URL("http://www.a.com"), new URL("http://a.com")));
        assertFalse(FetchHttpConnection.isDomainOrSubDomain(new URL("http://bob.uk.com"), new URL("http://uk.com")));
        assertFalse(FetchHttpConnection.isDomainOrSubDomain(new URL("http://bob.uk.com"), new URL("http://xyz.uk.com")));
    }

    @Test(timeout = 5000)
    public void testDontForwardSensitiveHeadersToDifferentProtocol() {
        String out = async(
            "const res = await fetch('https://httpbin.org/redirect-to?url=http%3A%2F%2Fhttpbin.org%2Fget&status_code=302', {" +
            "headers: {" +
                "'authorization': 'gets=removed'," +
                "'cookie': 'gets=removed'," +
                "'cookie2': 'gets=removed'," +
                "'www-authenticate': 'gets=removed'," +
                "'safe-header': 'gets=forwarded'" +
            "}" +
            "});" +
            "const headers = new Headers((await res.json()).headers);" +
            log("headers.has('authorization')") +
            log("headers.has('cookie')") +
            log("headers.has('cookie2')") +
            log("headers.has('www-authenticate')") +
            log("headers.get('safe-header')")
        );
        assertEquals("false\nfalse\nfalse\nfalse\ngets=forwarded\n", out);
    }

    @Test
    public void testIsSameProtocol() throws MalformedURLException {
        // forward headers to same protocol
        assertTrue(FetchHttpConnection.isSameProtocol(new URL("http://a.com"), new URL("http://a.com")));
        assertTrue(FetchHttpConnection.isSameProtocol(new URL("https://a.com"), new URL("https://a.com")));
        // dont forward to different protocol
        assertFalse(FetchHttpConnection.isSameProtocol(new URL("http://b.com"), new URL("https://b.com")));
        assertFalse(FetchHttpConnection.isSameProtocol(new URL("http://a.com"), new URL("https://www.a.com")));
    }

    @Test
    public void testBrokenRedirectNormalResponseInFollowMode() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/no-location');" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/no-location\n301\nundefined\n", out);
    }

    @Test
    public void testBrokenRedirectNormalResponseInManualMode() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/no-location', { redirect: 'manual' });" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/no-location\n301\nundefined\n", out);
    }

    @Test
    public void testRejectInvalidRedirect() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/redirect/301/invalid-url');"
        );
        assertEquals("invalid url in location header\n", out);
    }

    @Test
    public void testProcessInvalidRedirectInManualMode() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301/invalid-url', { redirect: 'manual' });" +
            log("res.url") +
            log("res.status") +
            log("res.headers.get('location')")
        );
        assertEquals("http://localhost:8080/redirect/301/invalid-url\n301\n//super:invalid:url%/\n", out);
    }

    @Test
    public void testSetRedirectedPropertyWhenRedirected() {
        String out = async(
            "const res = await fetch('http://localhost:8080/redirect/301/');" +
            log("res.url") +
            log("res.status") +
            log("res.redirected")
        );
        assertEquals("http://localhost:8080/inspect\n200\ntrue\n", out);
    }

    @Test
    public void testDontSetRedirectedPropertyWithoutRedirect() {
        String out = async(
            "const res = await fetch('http://localhost:8080/200');" +
            log("res.url") +
            log("res.redirected")
        );
        assertEquals("http://localhost:8080/200\nfalse\n", out);
    }

    @Test
    public void testHandleDNSErrorResponse() {
        String out = async(
            "const res = await fetch('http://domain.invalid');"
        );
        assertEquals("", out);
    }

    @Test
    public void testRejectInvalidJsonResponse() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/error/json');" +
            log("res.headers.get('content-type')") +
            "const result = await res.json();"
        );
        assertEquals("application/json\nUnexpected token < in JSON at position 0\n", out);
    }

    @Test
    public void testResponseWithoutStatusText() {
        String out = async(
            "const res = await fetch('http://localhost:8080/no-status-text');" +
            log("res.statusText === ''")
        );
        assertEquals("true\n", out);
    }

    @Test
    public void testNoContentResponse204() {
        String out = async(
            "const res = await fetch('http://localhost:8080/no-content');" +
            log("res.status, res.statusText, res.ok") +
            "const result = await res.text();" +
            log("result === ''")
        );
        assertEquals("204 No Content true\ntrue\n", out);
    }

    @Test
    public void testNotModifierResponse304() {
        String out = async(
            "const res = await fetch('http://localhost:8080/not-modified');" +
            log("res.status, res.statusText, res.ok") +
            "const result = await res.text();" +
            log("result === ''")
        );
        assertEquals("304 Not Modified false\ntrue\n", out);
    }

    @Test
    public void testSetDefaultUserAgent() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect');" +
            "const result = await res.json();" +
            log("result.headers['user-agent']")
        );
        assertEquals("graaljs-fetch\n", out);
    }

    @Test
    public void testSetCustomUserAgent() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', {" +
            "headers: { 'user-agent': 'faked' }" +
            "});" +
            "const result = await res.json();" +
            log("result.headers['user-agent']")
        );
        assertEquals("faked\n", out);
    }

    @Test
    public void testSetDefaultAcceptHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect');" +
            "const result = await res.json();" +
            log("result.headers['accept']")
        );
        assertEquals("*/*\n", out);
    }

    @Test
    public void testSetCustomAcceptHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', {" +
            "headers: { 'accept': 'application/json' }" +
            "});" +
            "const result = await res.json();" +
            log("result.headers['accept']")
        );
        assertEquals("application/json\n", out);
    }

    @Test
    public void testPOSTRequest() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'POST' });" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '0'")
        );
        assertEquals("POST\ntrue\n", out);
    }

    @Test
    public void testPOSTRequestWithStringBody() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'POST', body: 'a=1' });" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '3'") +
            log("result.headers['content-type'] === 'text/plain;charset=UTF-8'")
        );
        assertEquals("POST\ntrue\ntrue\n", out);
    }

    @Test
    public void testPOSTRequestWithObjectBody() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'POST', body: {a: 1} });" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '15'") +
            log("result.headers['content-type'] === 'text/plain;charset=UTF-8'")
        );
        assertEquals("POST\ntrue\ntrue\n", out);
    }

    @Test
    public void testShouldOverriteContentLengthIfAble() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'POST', body: 'a=1', headers: { 'Content-Length': '1000' } });" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '3'") +
            log("result.headers['content-type'] === 'text/plain;charset=UTF-8'")
        );
        assertEquals("POST\ntrue\ntrue\n", out);
    }

    @Test
    public void testShouldAllowPUTRequest() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'PUT', body: 'a=1'});" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '3'") +
            log("result.headers['content-type'] === 'text/plain;charset=UTF-8'")
        );
        assertEquals("PUT\ntrue\ntrue\n", out);
    }

    @Test
    public void testShouldAllowDELETERequest() {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'DELETE' });" +
            "const result = await res.json();" +
            log("result.method")
        );
        assertEquals("DELETE\n", out);
    }

    @Test
    public void testShouldAllowDELETERequestWithBody() throws InterruptedException {
        String out = async(
            "const res = await fetch('http://localhost:8080/inspect', { method: 'DELETE', body: 'a=1' });" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['content-length'] === '3'") +
            log("result.body")
        );
        assertEquals("DELETE\ntrue\na=1\n", out);
    }

    @Test
    public void testShouldAllowHEADRequestWithContentEncodingHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/error/404', { method: 'HEAD' });" +
            log("res.status") +
            log("res.headers.get('content-encoding')")
        );
        assertEquals("404\ngzip\n", out);
    }

    @Test
    public void testShouldAllowOPTIONSRequest() {
        String out = async(
            "const res = await fetch('http://localhost:8080/options', { method: 'OPTIONS' });" +
            log("res.status") +
            log("res.headers.get('allow')")
        );
        assertEquals("200\nGET, HEAD, OPTIONS\n", out);
    }

    @Test
    public void testShouldRejectConsumingBodyTwice() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/plain');" +
            log("res.headers.get('content-type')") +
            "await res.text();" +
            log("res.bodyUsed") +
            "await res.text()"
        );
        assertEquals("text/plain\ntrue\nBody already used\n", out);
    }

    @Test
    public void testRejectCloneAfterBodyConsumed() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:8080/plain');" +
            log("res.headers.get('content-type')") +
            "await res.text();" +
            "const res2 = res.clone();"
        );
        assertEquals("text/plain\ncannot clone body after it is used\n", out);
    }

    @Test
    public void testAllowCloningResponseAndReadBothBodies() {
        String out = async(
            "const res = await fetch('http://localhost:8080/plain');" +
            log("res.headers.get('content-type')") +
            "const res2 = res.clone();" +
            "await res.text();" +
            "await res2.text();" +
            log("res.bodyUsed, res2.bodyUsed")
        );
        assertEquals("text/plain\ntrue true\n", out);
    }

    @Test
    public void testGetAllResponseValuesOfHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/cookie');" +
            log("res.headers.get('set-cookie')") +
            log("res.headers.get('Set-Cookie')")
        );
        assertEquals("a=1, b=2\na=1, b=2\n", out);
    }

    @Test
    public void testDeleteHeader() {
        String out = async(
            "const res = await fetch('http://localhost:8080/cookie');" +
            log("res.headers.has('set-cookie')") +
            "res.headers.delete('set-cookie');" +
            log("res.headers.has('set-cookie')")
        );
        assertEquals("true\nfalse\n", out);
    }

    @Test
    public void testFetchWithRequestInstance() {
        String out = async(
            "const req = new Request('http://localhost:8080/200');" +
            "const res = await fetch(req);" +
            log("res.url")
        );
        assertEquals("http://localhost:8080/200\n", out);
    }

    @Test
    public void testFetchOptionsOverwriteRequestInstance() {
        String out = async(
            "const req = new Request('http://localhost:8080/inspect', { method: 'POST', headers: {a:'1'} });" +
            "const res = await fetch(req, { method: 'GET', headers: {a:'2'} } );" +
            "const result = await res.json();" +
            log("result.method") +
            log("result.headers['a'] === '2'")
        );
        assertEquals("GET\ntrue\n", out);
    }

    @Test
    public void testKeepQuestionMarkWithoutParams() {
        String out = async(
            "const res = await fetch('http://localhost:8080/question?');" +
            log("res.url")
        );
        assertEquals("http://localhost:8080/question?\n", out);
    }

    @Test
    public void testKeepUrlParams() {
        String out = async(
            "const res = await fetch('http://localhost:8080/question?a=1');" +
            log("res.url")
        );
        assertEquals("http://localhost:8080/question?a=1\n", out);
    }

    @Test
    public void testKeepHashSymbol() {
        String out = async(
            "const res = await fetch('http://localhost:8080/question?#');" +
            log("res.url")
        );
        assertEquals("http://localhost:8080/question?#\n", out);
    }

    @Test(timeout = 5000)
    public void testSupportsHttps() {
        String out = async(
            "const res = await fetch('https://github.com/', {method: 'HEAD'});" +
            log("res.ok === true")
        );
        assertEquals("true\n", out);
    }

    @Test
    public void testCustomFetchError() {
        String out = async(
            "const sysErr = new Error('system');" +
            "const err = new FetchError('test message','test-type', sysErr);" +
            log("err.message") +
            log("err.type")
        );
        assertEquals("test message\ntest-type\n", out);
    }

    @Test
    public void testRejectNetworkFailure() {
        String out = asyncThrows(
            "const res = await fetch('http://localhost:50000');"
        );
        assertEquals("Connection refused\n", out);
    }

    @Test
    public void testExtractErroredSysCall() {
        String out = async(
            "try {" +
            "const res = await fetch('http://localhost:50000');" +
            "} catch (err) {" +
            log("err.message") +
            "}"
        );
        assertEquals("Connection refused\n", out);
    }

    private String async(String test) {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(new TestOutput()).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                    "(async function () {" +
                    test +
                    "})");
            asyncFn.executeVoid();
        }
        return out.toString();
    }

    private String asyncThrows(String test) {
        TestOutput out = new TestOutput();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).err(new TestOutput()).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").build()) {
            Value asyncFn = context.eval(ID, "" +
                    "(async function () {" +
                    "try {" +
                    test +
                    "}" +
                    "catch (error) {" +
                    "console.log(error.message)" +
                    "}" +
                    "})");
            asyncFn.executeVoid();
        }
        return out.toString();
    }

    private String log(String code) {
        return "console.log(" + code + ");";
    }
}
