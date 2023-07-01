/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of the Request class.
 *
 * @option global-fetch
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const baseURL = "http://localhost:8080";

(function shouldConformToIDL() {
    const req = new Request(baseURL);
    const set = new Set();
    //request
    assertTrue(Reflect.has(set, 'size'))
    assertTrue(Reflect.has(req, 'method'));
    assertTrue(Reflect.has(req, 'url'));
    assertTrue(Reflect.has(req, 'headers'));
    assertTrue(Reflect.has(req, 'redirect'));
    assertTrue(Reflect.has(req, 'referrer'));
    assertTrue(Reflect.has(req, 'referrerPolicy'));
    assertTrue(Reflect.has(req, 'clone'));
    //body
    assertTrue(Reflect.has(req, 'body'));
    assertTrue(Reflect.has(req, 'bodyUsed'));
    assertTrue(Reflect.has(req, 'arrayBuffer'));
    assertTrue(Reflect.has(req, 'text'));
    assertTrue(Reflect.has(req, 'json'));
    assertTrue(Reflect.has(req, 'blob'));
    // currently unsupported:
    //assertTrue(Reflect.has(req, 'formData'));
})();

(function shouldSetCorrectDefaults() {
    const req = new Request(baseURL);
    assertSame(baseURL, req.url);
    assertSame('GET', req.method);
    assertSame('follow', req.redirect);
    assertSame('about:client', req.referrer);
    assertSame('', req.referrerPolicy);
    assertSame(null, req.body);
    assertSame(false, req.bodyUsed);
})();

(function shouldSupportWrappingOtherRequest() {
    const r1 = new Request(baseURL, {
        method: 'POST',
    });

    const r2 = new Request(r1, {
        method: 'POST2',
    });

    assertSame(baseURL, r1.url);
    assertSame('POST', r1.method);
    assertSame(baseURL, r2.url);
    assertSame('POST2', r2.method);
})();

(function shouldThrowErrorOnGETOrHEADWithBody() {
    assertThrows(() => new Request(baseURL, {body: ''}), TypeError);
    assertThrows(() => new Request(baseURL, {body: 'a'}), TypeError);
    assertThrows(() => new Request(baseURL, {method: 'HEAD', body: ''}), TypeError);
    assertThrows(() => new Request(baseURL, {method: 'HEAD', body: 'a'}), TypeError);
    assertThrows(() => new Request(baseURL, {method: 'head', body: ''}), TypeError);
    assertThrows(() => new Request(baseURL, {method: 'get', body: ''}), TypeError);
    assertThrows(() => new Request(new Request(baseURL, {body: ''})), TypeError);
    assertThrows(() => new Request(new Request(baseURL, {body: 'a'})), TypeError);
})();

(function shouldThrowErrorOnInvalidUrl() {
    assertThrows(() => new Request('foobar'), TypeError);
})();

(function shouldThrowErrorWhenUrlIncludesCredentials() {
    assertThrows(() => new Request('https://user:pass@github.com/'), TypeError);
})();

(function shouldDefaultToNullBody() {
    const req = new Request(baseURL);
    assertSame(null, req.body);
    req.text().then(result => assertSame('', result));
})();

(function shouldSupportParsingHeaders() {
    const req = new Request(baseURL, {
        headers: {
            a: '1',
            'b': 2,
        }
    });
    assertSame(baseURL, req.url);
    assertSame('1', req.headers.get('a'));
    assertSame('2', req.headers.get('b'));
})();

(function shouldAcceptHeadersInstance() {
    const headers = new Headers({
        'a': '1',
        'b': '2',
    });
    const req = new Request(baseURL, { headers });
    assertSame(baseURL, req.url);
    assertSame('1', req.headers.get('a'));
    assertSame('2', req.headers.get('b'));
})();

// https://fetch.spec.whatwg.org/#concept-method-normalize
(function shouldNormalizeMethod() {
    for (const method of ['DELETE', 'GET', 'HEAD', 'OPTIONS', 'POST', 'PUT']) {
        const request = new Request(baseURL, {
            method: method.toLowerCase()
        });
        assertSame(method, request.method);
    }

    for (const method of ['patch', 'FOO', 'bar']) {
        const request = new Request(baseURL, {method});
        assertSame(method, request.method);
    }
})();

(function shouldSupportTextMethod() {
    const request = new Request(baseURL, {
        method: 'POST',
        body: 'a=1'
    });

    assertSame(baseURL, request.url);
    return request.text().then(result => {
        assertSame('a=1', result);
    });
})();

(function shouldSupportJsonMethod() {
    const request = new Request(baseURL, {
        method: 'POST',
        body: '{"a":1}'
    });

    assertSame(baseURL, request.url);
    request.json().then(result => {
        assertSame(1, result.a);
    });
})();

(function shouldSupportBlobMethod() {
    new Request(baseURL).blob().then(b => b.text());
    new Request(baseURL).blob().then(b => b.arrayBuffer());
    new Request(baseURL, {method: 'POST', body: 'a=1'}).blob().then(b => b.text());
    // currently unsupported:
    new Request(baseURL, {method: 'POST', body: 'a=1'}).blob().then(b => b.arrayBuffer())
        .then(() => {throw new Error()}).catch((e) => {if (!(e instanceof TypeError)) throw new Error("Expected TypeError")});
})();

(function shouldSupportFormDataMethod() {
    // .formData() not implemented
    const request = new Request(baseURL);
    assertThrows(() => request.formData(), TypeError);
})();

(function shouldSupportCloneMethod() {
    const request = new Request(baseURL, {
        method: 'POST',
        redirect: 'manual',
        headers: {
            a: '1'
        },
        body: 'b=2'
    });

    const clone = request.clone();
    assertFalse(request === clone);
    assertSame(baseURL, clone.url);
    assertSame('POST', clone.method);
    assertSame('manual', clone.redirect);
    assertSame('1', clone.headers.get('a'));
    Promise.all([request.text(), clone.text()]).then(results => {
        assertSame('b=2', results[0]);
        assertSame('b=2', results[1]);
    });
})();

(function shouldThrowErrorOnForbiddenMethod() {
    for (const method of ['CONNECT', 'TRACE', 'TRACK']) {
        assertThrows(() => new Request(baseURL, {method}), TypeError);
        assertThrows(() => new Request(baseURL, {method: method.toLowerCase()}), TypeError);
    }
})();

function stringToBytes(s) {
    return Uint8Array.from([...s].map(s => s.charCodeAt(0))).buffer;
}

(function shouldSupportArrayBufferMethod() {
    const request = new Request(baseURL, {
        method: 'POST',
        body: stringToBytes('a=1')
    });

    assertSame(baseURL, request.url);
    request.arrayBuffer().then(result => {
        assertTrue(result instanceof ArrayBuffer);
        const string = String.fromCharCode.apply(null, new Uint8Array(result));
        assertSame('a=1', string);
    });
})();
