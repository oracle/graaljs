/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.zip() and Iterator.zipKeyed() with foreign objects.
 *
 * @option ecmascript-version=staging
 */

load("./iteratorhelper_common.js");

{
  let foreignIterables = java.util.List.of(java.util.List.of(1, 2), java.util.List.of(3));
  let iter = Iterator.zip(foreignIterables, {mode: "longest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSameContent([1, 3], result.value);

  result = iter.next();
  assertSame(false, result.done);
  assertSameContent([2, undefined], result.value);

  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let ProxyObject = Java.type("org.graalvm.polyglot.proxy.ProxyObject");
  let LinkedHashMap = Java.type("java.util.LinkedHashMap");
  let foreignIterables = new LinkedHashMap();
  foreignIterables.put("a", java.util.List.of(1, 2));
  foreignIterables.put("b", java.util.List.of(3));

  let iter = Iterator.zipKeyed(ProxyObject.fromMap(foreignIterables), {mode: "longest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSame(null, Object.getPrototypeOf(result.value));
  assertSameContent(["a", "b"], Object.keys(result.value));
  assertSame(1, result.value.a);
  assertSame(3, result.value.b);

  result = iter.next();
  assertSame(false, result.done);
  assertSame(null, Object.getPrototypeOf(result.value));
  assertSameContent(["a", "b"], Object.keys(result.value));
  assertSame(2, result.value.a);
  assertSame(undefined, result.value.b);

  assertIterResult({done: true, value: undefined}, iter.next());
}
