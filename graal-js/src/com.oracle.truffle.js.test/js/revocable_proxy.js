/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */


load('assert.js');

function isConstructor(fn) {
    try {
        new new Proxy(fn, { construct: () => ({}) });
        return true;
    } catch (e) {
        return false;
    }
}

[class C {}, new Proxy(class C {}, {})].map(function testRevokedProxyConstructible(target) {
    let {proxy, revoke} = Proxy.revocable(target, {});
    assertTrue(isConstructor(proxy));
    revoke();
    assertTrue(isConstructor(proxy));
    try {
        new proxy();
        fail("should have thrown");
    } catch (e) {
        if (e.message.includes("not a function") || !e.message.includes("revoked")) {
            throw e;
        }
    }
});
