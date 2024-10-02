/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test non-networking behaviour of the fetch api.
 *
 * @option unhandled-rejections=throw
 */

load('./fetch-common.js');

const url = "http://localhost:8080";

(function shouldExposeFetchHeaderRequestResponse() {
    assertTrue('fetch' in globalThis);
    assertTrue('Headers' in globalThis);
    assertTrue('Request' in globalThis);
    assertTrue('Response' in globalThis);
})();

(function shouldSupportNew() {
    assertTrue(new Headers() instanceof Headers);
    assertTrue(new Request(url) instanceof Request);
    assertTrue(new Response() instanceof Response);
})();

(function shouldThrowWithoutNew() {
    assertThrows(() => Headers(), TypeError);
    assertThrows(() => Request(url), TypeError);
    assertThrows(() => Response(), TypeError);
})();

(function shouldSupportProperStringOutput() {
    assertSame('[object Headers]', new Headers().toString());
    assertSame('[object Request]', new Request(url).toString());
    assertSame('[object Response]', new Response().toString());
})();

(function shouldThrowWithInvalidArgument() {
    // init/options must be undefined, null, or an object.
    assertThrows(() => new Headers(42), TypeError);
    assertThrows(() => new Request(url, 42), TypeError);
    assertThrows(() => new Response(url, 42), TypeError);
    fetch(url, 42).then(() => {throw new Error()}).catch((e) => {if (!(e instanceof TypeError)) throw new Error("Expected TypeError for non-object argument")});
})();

(function shouldHaveCorrectFunctionLength() {
    assertSame(1, fetch.length);
    assertSame(1, Request.length);
    assertSame(0, Response.length);
    assertSame(0, Headers.length);
})();
