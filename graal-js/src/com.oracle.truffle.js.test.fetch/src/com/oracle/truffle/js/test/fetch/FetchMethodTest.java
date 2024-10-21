/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.interop.AsyncInteropTest.TestOutput;

/**
 * Tests for the fetch builtin.
 */
public class FetchMethodTest extends JSTest {
    private static final int TEST_TIMEOUT = 10000;

    private static Source fetchSource;

    private FetchTestServer localServer;

    @BeforeClass
    public static void testSetup() throws IOException {
        // allow overwriting restricted headers
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length");

        fetchSource = Source.newBuilder(ID, FetchMethodTest.class.getResource("fetch.js")).build();
    }

    @Before
    public void startServer() throws IOException {
        localServer = new FetchTestServer(8080);
        localServer.start();
    }

    @After
    public void stopServer() {
        localServer.stop();
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testReturnsPromise() {
        String out = async("""
                        const res = fetch('http://localhost:8080/200');
                        console.log(res instanceof Promise);
                        """);
        assertEquals("true\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void test200() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/200');
                        console.log(res.ok, res.status, res.statusText);
                        """);
        assertEquals("true 200 OK\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testHandleClientErrorResponse() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/error/400');
                        console.log(res.headers.get('content-type'));
                        console.log(res.status);
                        console.log(res.statusText);
                        const result = await res.text();
                        console.log(result);
                        """);
        assertEquals("text/plain\n400\nBad Request\nclient error\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testHandleServerErrorResponse() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/error/500');
                        console.log(res.headers.get('content-type'));
                        console.log(res.status);
                        console.log(res.statusText);
                        const result = await res.text();
                        console.log(result);
                        """);
        assertEquals("text/plain\n500\nInternal Server Error\nserver error\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testResolvesIntoResponseObject() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/200');
                        console.log(res instanceof Response);
                        console.log(res.headers instanceof Headers);
                        console.log(res.status);
                        console.log(res.ok);
                        console.log(res.statusText);
                        """);
        assertEquals("true\ntrue\n200\ntrue\nOK\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectUnsupportedProtocol() {
        String out = asyncThrows(
                        "const res = await fetch('ftp://example.com/');");
        assertEquals("fetch cannot load ftp://example.com/. Scheme not supported: ftp\n", out);
    }

    @Test
    public void testDataUrlProcessor() {
        String out = async("""
                        let res;
                        res = await fetch('data:,helloworld');
                        console.log(await res.text());
                        res = await fetch('data:text/plain,helloworld');
                        console.log(await res.text());
                        """);
        assertEquals("""
                        helloworld
                        helloworld
                        """, out);
    }

    @Test
    public void testBase64DataUrlEncoding() {
        String out = async("""
                        let res;
                        res = await fetch('data:text/plain;base64,aGVsbG93b3JsZA');
                        console.log(await res.text());
                        res = await fetch('data:;base64,aGVsbG93b3JsZA==');
                        console.log(await res.text());
                        """);
        assertEquals("""
                        helloworld
                        helloworld
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectOnNetworkFailure() {
        String out = asyncThrows(
                        "const res = await fetch('http://localhost:50000');");
        assertEquals("Connection refused\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testAcceptPlainTextResponse() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/plain');
                        const result = await res.text();
                        console.log(res.headers.get('content-type'));
                        console.log(res.bodyUsed);
                        console.log(result);
                        """);
        assertEquals("""
                        text/plain;charset=UTF-8
                        true
                        text\uD83D\uDCA9
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testConvertTextUsingContentTypeCharset() {
        String out = async("""
                        let res = await fetch('http://localhost:8080/plain-utf-16');
                        let result = await res.text();
                        console.log(res.headers.get('content-type'));
                        console.log(result);

                        res = await fetch('http://localhost:8080/plain-utf-32');
                        result = await res.text();
                        console.log(res.headers.get('content-type'));
                        console.log(result);
                        """);
        assertEquals("""
                        text/plain;charset=UTF-16
                        text\uD83D\uDCA9
                        text/plain;charset=UTF-32
                        text\uD83D\uDCA9
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testAcceptHtmlResponse() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/html');
                        const result = await res.text();
                        console.log(res.headers.get('content-type'));
                        console.log(res.bodyUsed);
                        console.log(result);
                        """);
        assertEquals("text/html\ntrue\n<html></html>\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testAcceptJsonResponse() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/json');
                        const result = await res.json();
                        console.log(res.headers.get('content-type'));
                        console.log(res.bodyUsed);
                        console.log(result.name);
                        """);
        assertEquals("application/json\ntrue\nvalue\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSendRequestWithCustomHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                           headers: { 'x-custom-header': 'abc' }
                        });
                        const result = await res.json();
                        console.log(result.headers['x-custom-header']);
                        """);
        assertEquals("abc\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testCustomHostHeader() {
        // Host is a restricted header.
        Assume.assumeTrue(System.getProperty("jdk.httpclient.allowRestrictedHeaders", "").toLowerCase(Locale.ROOT).contains("host"));
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                           headers: { 'host': 'example.com' }
                        });
                        const result = await res.json();
                        console.log(result.headers.host);
                        """);
        assertEquals("example.com\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testCustomMethod() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {method: 'foo'});
                        const result = await res.json();
                        console.log(result.method === 'foo');
                        """);
        assertEquals("true\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectCode301() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectCode302() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/302');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectCode303() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/303');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectCode307() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/307');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectCode308() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/308');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectChain() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/chain');
                        console.log(res.url, res.status);
                        """);
        assertEquals("http://localhost:8080/inspect 200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowPOSTRequestRedirectWithGET() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        method: 'POST'
                        });
                        const result = await res.json();
                        console.log(res.url, res.status);
                        console.log(result.method, result.body === '');
                        """);
        assertEquals("http://localhost:8080/inspect 200\nGET true\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowPATCHRequestRedirectWithPATCH() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        method: 'PATCH'
                        });
                        const result = await res.json();
                        console.log(res.url, res.status);
                        console.log(result.method, result.body === '');
                        """);
        assertEquals("""
                        http://localhost:8080/inspect 200
                        PATCH true
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollow303WithGET() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/303', {
                        method: 'PUT'
                        });
                        const result = await res.json();
                        console.log(res.url, res.status);
                        console.log(result.method, result.body === '');
                        """);
        assertEquals("""
                        http://localhost:8080/inspect 200
                        GET true
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testMaximumFollowsReached() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/redirect/chain', {
                        follow: 1
                        });
                        """);
        assertEquals("maximum redirect reached at: http://localhost:8080/redirect/301\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testNoRedirectsAllowed() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        follow: 0
                        });
                        """);
        assertEquals("maximum redirect reached at: http://localhost:8080/redirect/301\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectModeManual() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        redirect: 'manual'
                        });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("""
                        http://localhost:8080/redirect/301
                        301
                        /inspect
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectModeManualBrokenLocationHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/bad-location', {
                        redirect: 'manual'
                        });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("http://localhost:8080/redirect/bad-location\n301\n<>\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectModeManualOtherHost() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/other-host', {
                        redirect: 'manual'
                        });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("http://localhost:8080/redirect/other-host\n301\nhttps://github.com/oracle/graaljs\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectModeManualNoRedirect() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/200', {
                        redirect: 'manual'
                        });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.has('location'));
                        """);
        assertEquals("http://localhost:8080/200\n200\nfalse\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRedirectModeError() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        redirect: 'error'
                        });
                        """);
        assertEquals("uri requested responds with a redirect, redirect mode is set to error\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFollowRedirectAndKeepHeaders() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        headers: {'x-custom-header': 'abc'}
                        });
                        const result = await res.json();
                        console.log(res.url);
                        console.log(result.headers['x-custom-header']);
                        """);
        assertEquals("http://localhost:8080/inspect\nabc\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testDontForwardSensitiveToDifferentHost() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301?http://127.0.0.1:8080/inspect', {
                        headers: {
                        'authorization': 'gets=removed',
                        'cookie': 'gets=removed',
                        'cookie2': 'gets=removed',
                        'www-authenticate': 'gets=removed',
                        'safe-header': 'gets=forwarded'
                        }
                        });
                        const headersJson = (await res.json()).headers;
                        const headers = new Headers(headersJson);
                        console.log(headers.has('authorization'));
                        console.log(headers.has('cookie'));
                        console.log(headers.has('cookie2'));
                        console.log(headers.has('www-authenticate'));
                        console.log(headers.get('safe-header'));
                        """);
        assertEquals("""
                        false
                        false
                        false
                        false
                        gets=forwarded
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testForwardSensitiveHeadersToSameHost() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301', {
                        headers: {
                        'authorization': 'gets=forwarded',
                        'cookie': 'gets=forwarded',
                        'cookie2': 'gets=forwarded',
                        'www-authenticate': 'gets=forwarded',
                        'safe-header': 'gets=forwarded'
                        }
                        });
                        const headers = new Headers((await res.json()).headers);
                        console.log(headers.get('authorization'));
                        console.log(headers.get('cookie'));
                        console.log(headers.get('cookie2'));
                        console.log(headers.get('www-authenticate'));
                        console.log(headers.get('safe-header'));
                        """);
        assertEquals("""
                        gets=forwarded
                        gets=forwarded
                        gets=forwarded
                        gets=forwarded
                        gets=forwarded
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testIsDomainOrSubDomain() throws URISyntaxException {
        // forward headers to same (sub)domain
        assertTrue(isDomainOrSubDomain(url("http://a.com"), url("http://a.com")));
        assertTrue(isDomainOrSubDomain(url("http://a.com"), url("http://www.a.com")));
        assertTrue(isDomainOrSubDomain(url("http://a.com"), url("http://foo.bar.a.com")));
        // dont forward to parent domain, another sibling or a unrelated domain
        assertFalse(isDomainOrSubDomain(url("http://b.com"), url("http://a.com")));
        assertFalse(isDomainOrSubDomain(url("http://www.a.com"), url("http://a.com")));
        assertFalse(isDomainOrSubDomain(url("http://bob.uk.com"), url("http://uk.com")));
        assertFalse(isDomainOrSubDomain(url("http://bob.uk.com"), url("http://xyz.uk.com")));
    }

    @Ignore("TODO Mock https server")
    @Test(timeout = TEST_TIMEOUT)
    public void testDontForwardSensitiveHeadersToDifferentProtocol() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/302?https://localhost:8081/inspect', {
                        headers: {
                        'authorization': 'gets=removed',
                        'cookie': 'gets=removed',
                        'cookie2': 'gets=removed',
                        'www-authenticate': 'gets=removed',
                        'safe-header': 'gets=forwarded'
                        }
                        });
                        const headers = new Headers((await res.json()).headers);
                        console.log(headers.has('authorization'));
                        console.log(headers.has('cookie'));
                        console.log(headers.has('cookie2'));
                        console.log(headers.has('www-authenticate'));
                        console.log(headers.get('safe-header'));
                        """);
        assertEquals("""
                        false
                        false
                        false
                        false
                        gets=forwarded
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testIsSameProtocol() throws URISyntaxException {
        // forward headers to same protocol
        assertTrue(isSameProtocol(url("http://a.com"), url("http://a.com")));
        assertTrue(isSameProtocol(url("https://a.com"), url("https://a.com")));
        // dont forward to different protocol
        assertFalse(isSameProtocol(url("http://b.com"), url("https://b.com")));
        assertFalse(isSameProtocol(url("http://a.com"), url("https://www.a.com")));
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testBrokenRedirectNormalResponseInFollowMode() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/no-location');
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("""
                        http://localhost:8080/redirect/no-location
                        301
                        null
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testBrokenRedirectNormalResponseInManualMode() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/no-location', { redirect: 'manual' });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("""
                        http://localhost:8080/redirect/no-location
                        301
                        null
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectInvalidRedirect() {
        String out = asyncThrows(
                        "const res = await fetch('http://localhost:8080/redirect/301/invalid-url');");
        assertEquals("invalid url in location header\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testProcessInvalidRedirectInManualMode() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301/invalid-url', { redirect: 'manual' });
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.headers.get('location'));
                        """);
        assertEquals("http://localhost:8080/redirect/301/invalid-url\n301\n//super:invalid:url%/\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSetRedirectedPropertyWhenRedirected() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/redirect/301/');
                        console.log(res.url);
                        console.log(res.status);
                        console.log(res.redirected);
                        """);
        assertEquals("""
                        http://localhost:8080/inspect
                        200
                        true
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testDontSetRedirectedPropertyWithoutRedirect() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/200');
                        console.log(res.url);
                        console.log(res.redirected);
                        """);
        assertEquals("http://localhost:8080/200\nfalse\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testHandleDNSErrorResponse() {
        // Ignore this test on darwin-amd64 due to transient failures.
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch");
        Assume.assumeFalse((osName.startsWith("mac") || osName.startsWith("darwin")) && (osArch.equals("amd64") || osArch.equals("x86_64")));

        String out = async("""
                        try {
                            await fetch('http://domain.invalid');
                            throw new Error("should have thrown");
                        } catch (err) {
                            console.log(`${err.name}: ${err.message}`);
                        }
                        """);
        assertThat(out, startsWith("TypeError"));
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectInvalidJsonResponse() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/error/json');
                        console.log(res.headers.get('content-type'));
                        const result = await res.json();
                        """);
        assertEquals("application/json\nUnexpected token < in JSON at position 0\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testResponseWithoutStatusText() {
        String out = async("""
                        try {
                            await fetch('http://localhost:8080/no-status-text');
                            throw new Error("should have thrown");
                        } catch (err) {
                            console.log(err.name);
                        }
                        """);
        assertEquals("TypeError\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testNoContentResponse204() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/no-content');
                        console.log(res.status, res.statusText, res.ok);
                        const result = await res.text();
                        console.log(`'${result}'`);
                        """);
        assertEquals("""
                        204 No Content true
                        ''
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testNotModifierResponse304() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/not-modified');
                        console.log(res.status, res.statusText, res.ok);
                        const result = await res.text();
                        console.log(result === '');
                        """);
        assertEquals("304 Not Modified false\ntrue\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSetDefaultUserAgent() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect');
                        const result = await res.json();
                        console.log(result.headers['user-agent']);
                        """);
        assertEquals("graaljs-fetch\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSetCustomUserAgent() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                        headers: { 'user-agent': 'faked' }
                        });
                        const result = await res.json();
                        console.log(result.headers['user-agent']);
                        """);
        assertEquals("faked\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSetDefaultAcceptHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect');
                        const result = await res.json();
                        console.log(result.headers['accept']);
                        """);
        assertEquals("*/*\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testSetCustomAcceptHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                        headers: { 'accept': 'application/json' }
                        });
                        const result = await res.json();
                        console.log(result.headers['accept']);
                        """);
        assertEquals("application/json\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testPOSTRequest() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', { method: 'POST' });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.headers['content-length'] === '0');
                        """);
        assertEquals("POST\ntrue\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testPOSTRequestWithStringBody() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', { method: 'POST', body: 'a=1' });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.headers['content-length'] === '3');
                        console.log(result.headers['content-type'] === 'text/plain;charset=UTF-8');
                        """);
        assertEquals("POST\ntrue\ntrue\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testPOSTRequestWithObjectBody() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                            method: 'POST',
                            body: {a: 1},
                            headers: [['Content-Type', 'text/plain;charset=UTF-8']],
                        });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.body);
                        console.log(result.headers['content-length']);
                        console.log(result.headers['content-type']);
                        """);
        assertEquals("""
                        POST
                        [object Object]
                        15
                        text/plain;charset=UTF-8
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldOverwriteContentLengthIfAble() {
        Assume.assumeTrue(System.getProperty("jdk.httpclient.allowRestrictedHeaders", "").toLowerCase(Locale.ROOT).contains("content-length"));
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', {
                            method: 'POST', body: 'a = 1;;;',
                            headers: { 'Content-Length': '5' }
                        });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.body);
                        console.log(result.headers['content-length']);
                        console.log(result.headers['content-type']);
                        """);
        assertEquals("""
                        POST
                        a = 1
                        5
                        text/plain;charset=UTF-8
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldAllowPUTRequest() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', { method: 'PUT', body: 'a=1' });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.headers['content-length']);
                        console.log(result.headers['content-type']);
                        """);
        assertEquals("""
                        PUT
                        3
                        text/plain;charset=UTF-8
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldAllowDELETERequest() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', { method: 'DELETE' });
                        const result = await res.json();
                        console.log(result.method);
                        """);
        assertEquals("DELETE\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldAllowDELETERequestWithBody() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/inspect', { method: 'DELETE', body: 'a=1' });
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.headers['content-length']);
                        console.log(result.body);
                        """);
        assertEquals("""
                        DELETE
                        3
                        a=1
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldAllowHEADRequestWithContentEncodingHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/error/404', { method: 'HEAD' });
                        console.log(res.status);
                        console.log(res.headers.get('content-encoding'));
                        """);
        assertEquals("404\ngzip\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldAllowOPTIONSRequest() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/options', { method: 'OPTIONS' });
                        console.log(res.status);
                        console.log(res.headers.get('allow'));
                        """);
        assertEquals("200\nGET, HEAD, OPTIONS\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testShouldRejectConsumingBodyTwice() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/plain');
                        console.log(res.headers.get('content-type'));
                        await res.text();
                        console.log(res.bodyUsed);
                        await res.text()
                        """);
        assertEquals("""
                        text/plain;charset=UTF-8
                        true
                        Body already used
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectCloneAfterBodyConsumed() {
        String out = asyncThrows("""
                        const res = await fetch('http://localhost:8080/plain');
                        console.log(res.headers.get('content-type'));
                        await res.text();
                        res.clone();
                        """);
        assertEquals("""
                        text/plain;charset=UTF-8
                        Body has already been consumed
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testAllowCloningResponseAndReadBothBodies() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/plain');
                        console.log(res.headers.get('content-type'));
                        const res2 = res.clone();
                        await res.text();
                        await res2.text();
                        console.log(res.bodyUsed, res2.bodyUsed);
                        """);
        assertEquals("text/plain;charset=UTF-8\ntrue true\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testGetAllResponseValuesOfHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/cookie');
                        console.log(res.headers.get('set-cookie'));
                        console.log(res.headers.get('Set-Cookie'));
                        """);
        assertEquals("a=1, b=2\na=1, b=2\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testResponseHeadersGetSetCookie() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/cookie2');
                        const headers = res.headers;
                        // all values of Set-Cookie, joined by ', '
                        console.log(headers.get('set-cookie'));
                        console.log(headers.get('Set-Cookie'));

                        // all values of Set-Cookie
                        console.log(headers.getSetCookie().join(';'));
                        console.log();

                        // all header values
                        headers.forEach((value, name, obj) => {
                            if (obj !== headers) {
                                throw new Error("Third argument must be this Headers object");
                            }
                            if (name === 'set-cookie') {
                                console.log(name + ': ' + value);
                            }
                        });
                        console.log('--');
                        for (const name of headers.keys()) {
                            if (name === 'set-cookie') {
                                console.log(name);
                            }
                        }
                        console.log('--');
                        for (const value of headers.values()) {
                            if (value.includes('=')) {
                                console.log(value);
                            }
                        }
                        console.log('--');
                        for (const [name, value] of headers.entries()) {
                            if (name === 'set-cookie') {
                                console.log(name + ': ' + value);
                            }
                        }
                        console.log('--');
                        for (const [name, value] of headers) {
                            if (name === 'set-cookie') {
                                console.log(name + ': ' + value);
                            }
                        }
                        """);
        assertEquals("""
                        a=1, b=2, c=3, d=4
                        a=1, b=2, c=3, d=4
                        a=1, b=2;c=3, d=4

                        set-cookie: a=1, b=2
                        set-cookie: c=3, d=4
                        --
                        set-cookie
                        set-cookie
                        --
                        a=1, b=2
                        c=3, d=4
                        --
                        set-cookie: a=1, b=2
                        set-cookie: c=3, d=4
                        --
                        set-cookie: a=1, b=2
                        set-cookie: c=3, d=4
                        """, out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testDeleteHeader() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/cookie');
                        console.log(res.headers.has('set-cookie'));
                        res.headers.delete('set-cookie');
                        console.log(res.headers.has('set-cookie'));
                        """);
        assertEquals("true\nfalse\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFetchWithRequestInstance() {
        String out = async("""
                        const req = new Request('http://localhost:8080/200');
                        const res = await fetch(req);
                        console.log(res.url);
                        """);
        assertEquals("http://localhost:8080/200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFetchOptionsOverwriteRequestInstance() {
        String out = async("""
                        const req = new Request('http://localhost:8080/inspect', { method: 'POST', headers: {a:'1'} });
                        const res = await fetch(req, { method: 'GET', headers: {a:'2'} } );
                        const result = await res.json();
                        console.log(result.method);
                        console.log(result.headers['a'] === '2');
                        """);
        assertEquals("GET\ntrue\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testKeepQuestionMarkWithoutParams() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/question?');
                        console.log(res.url);
                        """);
        assertEquals("http://localhost:8080/question?\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testKeepUrlParams() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/question?a=1');
                        console.log(res.url);
                        """);
        assertEquals("http://localhost:8080/question?a=1\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testKeepHashSymbol() {
        String out = async("""
                        const res = await fetch('http://localhost:8080/question?#');
                        console.log(res.url);
                        """);
        assertEquals("http://localhost:8080/question?#\n", out);
    }

    @Ignore
    @Test(timeout = TEST_TIMEOUT)
    public void testSupportsHttps() {
        String out = async("""
                        const res = await fetch('https://openjdk.org/', {method: 'HEAD'});
                        console.log(res.ok);
                        console.log(res.status);
                        """);
        assertEquals("true\n200\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testFetchUrlError() {
        String out = async("""
                        try {
                            await fetch('<invalid> "url"');
                        } catch (err) {
                            console.log(err.name);
                            console.log(err.message.toLowerCase().match('invalid url')?.[0]);
                        }
                        """);
        assertEquals("TypeError\ninvalid url\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testRejectNetworkFailure() {
        String out = asyncThrows(
                        "const res = await fetch('http://localhost:50000');");
        assertEquals("Connection refused\n", out);
    }

    @Test(timeout = TEST_TIMEOUT)
    public void testExtractErroredSysCall() {
        String out = async("""
                        try {
                            const res = await fetch('http://localhost:50000');
                        } catch (err) {
                            console.log(err.message);
                        }
                        """);
        assertEquals("Connection refused\n", out);
    }

    @Test
    public void testSupportsDataUrl() {
        String out = async("""
                        for await (let res of [
                                fetch('data:;base64,W3Rlc3QgZGF0YV0='),
                                fetch('data:text/plain;base64,W3Rlc3QgZGF0YV0='),
                                fetch('data:text/plain;charset=UTF-8;base64,W3Rlc3QgZGF0YV0='),
                                fetch('data:,%5Btest%20data%5D'),
                                fetch('data:text/plain,%5Btest%20data%5D'),
                            ]) {
                            console.log(res.ok && res.status, res.statusText);
                            console.log(res.headers.get('content-type'));
                            console.log(await res.text());
                        }
                        """);
        assertEquals("""
                        200 OK
                        text/plain;charset=US-ASCII
                        [test data]
                        200 OK
                        text/plain
                        [test data]
                        200 OK
                        text/plain;charset=UTF-8
                        [test data]
                        200 OK
                        text/plain;charset=US-ASCII
                        [test data]
                        200 OK
                        text/plain
                        [test data]
                        """, out);
    }

    private static String async(String test) {
        TestOutput out = new TestOutput();
        try (Context context = newContext(out)) {
            Value asyncFn = context.eval(ID, "" +
                            "(async function () {" +
                            test +
                            "})");
            asyncFn.executeVoid();
        }
        return out.toString();
    }

    private static String asyncThrows(String test) {
        TestOutput out = new TestOutput();
        try (Context context = newContext(out)) {
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

    private static Context newContext(TestOutput out) {
        var b = JSTest.newContextBuilder().err(out).out(out);
        b.allowHostAccess(explicitHostAccess());
        b.allowHostClassLookup((s) -> switch (s) {
            case "java.net.URI" -> true;
            case "java.net.http.HttpClient" -> true;
            case "java.net.http.HttpClient$Redirect" -> true;
            case "java.net.http.HttpRequest" -> true;
            case "java.net.http.HttpResponse" -> true;
            case "java.net.http.HttpRequest$BodyPublishers" -> true;
            case "java.net.http.HttpResponse$BodyHandlers" -> true;
            case "java.net.ConnectException" -> true;
            case "java.lang.String" -> true;
            case "java.nio.ByteBuffer" -> true;
            case "java.util.Base64" -> true;
            case "java.nio.charset.StandardCharsets" -> true;
            default -> false;
        });
        b.allowIO(IOAccess.ALL);
        b.option(JSContextOptions.CONSOLE_NAME, "true");
        b.option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw");
        Context context = b.build();
        context.eval(fetchSource);
        return context;
    }

    private static HostAccess explicitHostAccess() {
        var b = HostAccess.newBuilder(HostAccess.EXPLICIT);
        b.allowAccessInheritance(true);
        b.allowArrayAccess(true);
        b.allowListAccess(true);
        b.allowMapAccess(true);
        b.allowBufferAccess(true);
        var classesToAllow = List.of(
                        java.lang.String.class,
                        java.net.URI.class,
                        java.net.http.HttpClient.class,
                        java.net.http.HttpClient.Builder.class,
                        java.net.http.HttpClient.Redirect.class,
                        java.net.http.HttpHeaders.class,
                        java.net.http.HttpRequest.class,
                        java.net.http.HttpRequest.Builder.class,
                        java.net.http.HttpResponse.class,
                        java.net.http.HttpRequest.BodyPublishers.class,
                        java.net.http.HttpResponse.BodyHandlers.class,
                        java.nio.ByteBuffer.class,
                        java.nio.charset.StandardCharsets.class,
                        java.util.Base64.class,
                        java.util.Base64.Decoder.class,
                        java.util.Optional.class);
        for (var cls : classesToAllow) {
            for (var constructor : cls.getConstructors()) {
                b.allowAccess(constructor);
            }
            for (var method : cls.getMethods()) {
                b.allowAccess(method);
            }
            for (var field : cls.getFields()) {
                b.allowAccess(field);
            }
        }
        return b.build();
    }

    private static URI url(String url) throws URISyntaxException {
        return new URI(url);
    }

    public static boolean isDomainOrSubDomain(URI destination, URI origin) {
        String d = destination.getHost();
        String o = origin.getHost();
        return d.equals(o) || o.endsWith("." + d);
    }

    public static boolean isSameProtocol(URI destination, URI origin) {
        return destination.getScheme().equals(origin.getScheme());
    }
}
