/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

const jsFunction = function () {};
const proxyCallable = new Proxy(function() {}, {});
const foreignMethod = java.lang.System.getProperties;

[jsFunction, proxyCallable, foreignMethod].forEach((fn) => {
    const decorator = () => fn;
    const C = class {
        @decorator static #a() {};
        static getA() {
            return this.#a;
        };

        @decorator #b() {};
        getB() {
            return this.#b;
        };
    };

    assertSame(fn, C.getA());
    assertSame(fn, new C().getB());
});
