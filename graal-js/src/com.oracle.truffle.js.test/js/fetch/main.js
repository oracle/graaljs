/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test non-networking behaviour of the fetch api
 */

load('../assert.js');

const url = "http://localhost:8080";

(function shouldExposeHeaderRequestResponse() {
    assertTrue(new Headers() instanceof Headers);
    assertTrue(new Request(url) instanceof Request);
    assertTrue(new Response() instanceof Response);
})();

(function shouldSupportProperStringOutput () {
    assertSame('[object Headers]', new Headers().toString());
    assertSame('[object Request]', new Request(url).toString());
    assertSame('[object Response]', new Response().toString());
})();
