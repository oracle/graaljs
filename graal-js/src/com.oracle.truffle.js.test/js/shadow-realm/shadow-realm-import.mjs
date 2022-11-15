/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests ShadowRealm.prototype.importValue().
 *
 * @option shadow-realm=true
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const baseUrl = import.meta.url.substring(0, import.meta.url.lastIndexOf('/') + 1);
let moduleUrl = `${baseUrl}imported-module.mjs`;

function assertInstanceof(object, Constructor) {
    if (!(object instanceof Constructor)) {
        throw object;
    }
}

await (async function testImport() {
    const shadowRealm = new ShadowRealm();

    let defaultPromise = shadowRealm.importValue(moduleUrl, "default");
    assertInstanceof(defaultPromise, Promise);
    let defaultFun = await defaultPromise;
    assertSame("default return value", defaultFun());

    let add = await shadowRealm.importValue(moduleUrl, "add");
    assertSame(42, add(16, 26));
})();

await (async function testUnresolvableExport() {
    const shadowRealm = new ShadowRealm();

    try {
        await shadowRealm.importValue(moduleUrl, "undefined");
        fail("should have thrown an error");
    } catch (e) {
        assertInstanceof(e, TypeError);
    }
})();

await (async function testExportNameNotString() {
    const shadowRealm = new ShadowRealm();

    try {
        await shadowRealm.importValue(moduleUrl, new String("default"));
        fail("should have thrown an error");
    } catch (e) {
        assertInstanceof(e, TypeError);
    }

    await shadowRealm.importValue(new String(moduleUrl), "default");
})();

await (async function testNonExistentModule() {
    const shadowRealm = new ShadowRealm();

    let promise = shadowRealm.importValue("non-existent-module.mjs", "default");
    assertInstanceof(promise, Promise);
    try {
        await promise;
        fail("should have thrown an error");
    } catch (e) {
        assertInstanceof(e, TypeError);
    }
})();
