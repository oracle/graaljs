/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the handling of private symbols by proxies.
 * 
 * @option testV8-mode
 */

load("assert.js");

// Private symbols are handled even before the revocation check
let symbol = TestV8.createPrivateSymbol('foo');
let { proxy, revoke } = Proxy.revocable({}, {});
revoke();

// [[Set]]
assertSame(42, proxy[symbol] = 42);
assertThrows(function() { "use strict"; proxy[symbol] = 42 }, TypeError);
assertFalse(Reflect.set(proxy, symbol, 42));

// [[DefineOwnProperty]]
assertFalse(Reflect.defineProperty(proxy, symbol, {}));
assertThrows(() => Object.defineProperty(proxy, symbol, {}), TypeError);

// [[Delete]]
assertTrue(delete proxy[symbol]);
assertTrue((function() {"use strict"; return delete proxy[symbol]})());
assertTrue(Reflect.deleteProperty(proxy, symbol));

// [[GetOwnPropertyDescriptor]]
assertSame(undefined, Object.getOwnPropertyDescriptor(proxy, symbol));
assertSame(undefined, Reflect.getOwnPropertyDescriptor(proxy, symbol));
assertFalse(Object.prototype.hasOwnProperty.call(proxy, symbol));

// [[Has]]
assertFalse(symbol in proxy);
assertFalse(Reflect.has(proxy, symbol));

// [[Get]]
assertSame(undefined, proxy[symbol]);
assertSame(undefined, Reflect.get(proxy, symbol));
assertSame(undefined, Reflect.get(proxy, symbol, 42));
