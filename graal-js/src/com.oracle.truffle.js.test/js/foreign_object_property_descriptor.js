/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests own-property descriptor and enumerability operations on foreign objects.
 */

load("assert.js");

let ProxyObject = Java.type("org.graalvm.polyglot.proxy.ProxyObject");
let LinkedHashMap = Java.type("java.util.LinkedHashMap");
let members = new LinkedHashMap();
members.put("x", 42);

let foreignObject = ProxyObject.fromMap(members);

assertTrue(Object.prototype.propertyIsEnumerable.call(foreignObject, "x"));
assertFalse(Object.prototype.propertyIsEnumerable.call(foreignObject, "missing"));

let desc = Reflect.getOwnPropertyDescriptor(foreignObject, "x");
assertSame(42, desc.value);
assertTrue(desc.writable);
assertTrue(desc.enumerable);
assertTrue(desc.configurable);

assertSame(undefined, Reflect.getOwnPropertyDescriptor(foreignObject, "missing"));
