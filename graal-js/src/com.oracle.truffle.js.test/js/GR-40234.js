/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option foreign-object-prototype
 */

load("assert.js");

const javaObject = new java.lang.Object();
assertSame(Object.prototype.valueOf, javaObject.valueOf);
assertSame(Object.prototype.valueOf, javaObject['valueOf']);
assertSame(Object.prototype.valueOf, Reflect.get(javaObject, 'valueOf'));
assertSame(Object.prototype.valueOf, new Proxy(javaObject, {}).valueOf);
assertSame(Object.prototype.valueOf, Reflect.get(new Proxy(javaObject, {}), 'valueOf'));

assertTrue('valueOf' in javaObject);
assertTrue(Reflect.has(javaObject, 'valueOf'));
assertTrue('valueOf' in new Proxy(javaObject, {}));
assertTrue(Reflect.has(new Proxy(javaObject, {}), 'valueOf'));
