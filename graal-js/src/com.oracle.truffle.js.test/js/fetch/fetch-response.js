/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of the Response class.
 *
 * @option global-fetch
 * @option unhandled-rejections=throw
 */

load('../assert.js');

(function shouldConformToIDL() {
    const res = new Response();
    assertTrue(Reflect.has(res, 'type'));
    assertTrue(Reflect.has(res, 'url'));
    assertTrue(Reflect.has(res, 'redirected'));
    assertTrue(Reflect.has(res, 'status'));
    assertTrue(Reflect.has(res, 'statusText'));
    assertTrue(Reflect.has(res, 'ok'));
    assertTrue(Reflect.has(res, 'headers'));
    assertTrue(Reflect.has(res, 'clone'));
    //static
    ['error', 'redirect', 'json'].forEach(name => assertTrue(Object.getOwnPropertyNames(Response).includes(name)));
    //body
    assertTrue(Reflect.has(res, 'body'));
    assertTrue(Reflect.has(res, 'bodyUsed'));
    assertTrue(Reflect.has(res, 'arrayBuffer'));
    assertTrue(Reflect.has(res, 'blob'));
    assertTrue(Reflect.has(res, 'json'));
    assertTrue(Reflect.has(res, 'text'));
    // currently unsupported:
    //assertTrue(Reflect.has(res, 'formData'));
})();

(function shouldSupportEmptyOptions() {
    const res = new Response('a=1');
    return res.text().then(result => {
        assertSame('a=1', result);
    });
})();

(function shouldSupportParsingHeaders() {
    const res = new Response(null, {
        headers: {
            'a': '1',
            'b': '2',
        }
    });
    assertSame('1', res.headers.get('a'));
    assertSame('2', res.headers.get('b'));
})();

(function shouldSupportBlobMethod() {
    new Response().blob().then(b => b.text());
    new Response().blob().then(b => b.arrayBuffer());
    new Response('a=1').blob().then(b => b.text());
    // currently unsupported:
    new Response('a=1').blob().then(b => b.arrayBuffer())
        .then(() => {throw new Error()}).catch((e) => {if (!(e instanceof TypeError)) throw new Error("Expected TypeError")});
})();

(function shouldSupportFormDataMethod() {
    // .formData() not implemented
    const res = new Response('a=1');
    assertThrows(() => res.formData(), TypeError);
})();

(function shouldSupportJsonMethod() {
    const res = new Response('{"a":1}');
    return res.json().then(result => assertSame(1, result.a));
})();

(function shouldSupportTextMethod() {
    const res = new Response('a=1');
    return res.text().then(result => assertSame('a=1', result));
})();

(function shouldSupportCloneMethod() {
    const res = new Response('a=1', {
        headers: {
            b: '2'
        },
        url: 'http://localhost:8080',
        status: 346,
        statusText: 'production'
    });

    const clone = res.clone();
    assertFalse(res === clone);
    assertSame('2', clone.headers.get('b'));
    assertSame('http://localhost:8080', clone.url);
    assertSame(346, clone.status);
    assertSame('production', clone.statusText);
    assertFalse(clone.ok);

    return Promise.all([res.text(), clone.text()]).then(results => {
        assertSame('a=1', results[0]);
        assertSame('a=1', results[1]);
    });
})();

(function shouldDefaultToNullAsBody() {
    const res = new Response();
    assertSame(null, res.body);
    res.text().then(result => assertSame('', result));
})();

(function shouldDefaultTo200AsStatus() {
    const res = new Response();
    assertSame(200, res.status);
    assertTrue(res.ok);
})();

(function shouldDefaultToEmptyStringAsUrl() {
    const res = new Response();
    assertSame('', res.url);
})();

(function shouldDefaultToEmptyStringAsUrl() {
    const res = new Response();
    assertSame('', res.url);
})();

(function shouldSetDefaultType() {
    const res = new Response();
    assertSame('default', res.type);
})();

(function shouldSupportStaticErrorMethod() {
    const res = Response.error();
    assertTrue(res instanceof Response);
    assertSame('error', res.type);
    assertSame(0, res.status);
    assertSame('', res.statusText);
})();

(function shouldSupportStaticRedirectMethod() {
    const url = 'http://localhost:8080';
    const res = Response.redirect(url, 301);
    assertTrue(res instanceof Response);
    assertSame(url, res.headers.get('Location'));
    assertSame(301, res.status);
    // reject non-redirect codes
    assertThrows(() => Response.redirect(url, 200), RangeError);
    // reject invalid url
    assertThrows(() => Response.redirect('foobar', 200), TypeError);
})();

(function shouldSupportStaticJsonMethod() {
    const res = Response.json({key: 'value'});
    assertTrue(res instanceof Response);
    assertSame('application/json', res.headers.get('Content-Type'));
    assertSame(200, res.status);
    res.text().then(result => assertSame(JSON.stringify({key: 'value'}), result));
})();

function stringToBytes(s) {
    return Uint8Array.from([...s].map(s => s.charCodeAt(0))).buffer;
}

(function shouldSupportArrayBufferMethod() {
    const res = new Response(stringToBytes('a=1'));
    res.arrayBuffer().then(result => {
        const string = String.fromCharCode.apply(null, new Uint8Array(result));
        assertSame('a=1', string);
    });
})();
