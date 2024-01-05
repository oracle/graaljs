/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the consistency of delete, get and set operations on foreign objects.
 */

load("assert.js");

// Foreign array
const list = new java.util.ArrayList();
const listItem = 'someItem';
list.add(listItem);
const proxyList = new Proxy(list, {});

// delete
assertTrue(delete list['foo']);
assertTrue(Reflect.deleteProperty(list, 'foo'));
assertTrue(delete proxyList['foo']);

// get
assertSame(listItem, list[0]);
assertSame(listItem, Reflect.get(list, 0));
assertSame(listItem, proxyList[0]);

// set
const newValue = 'someValue';
list[0] = newValue;
assertSame(newValue, list[0]);
list[0] = listItem;

Reflect.set(list, 0, newValue);
assertSame(newValue, list[0]);
list[0] = listItem;

proxyList[0] = newValue;
assertSame(newValue, proxyList[0]);
assertSame(newValue, list[0]);
list[0] = listItem;

// Foreign map
const map = new java.util.HashMap();
const mapKey = 'klic';
const mapValue = 'hodnota';
map.put(mapKey, mapValue);
const proxyMap = new Proxy(map, {});

// delete
assertTrue(delete map['foo']);
assertTrue(Reflect.deleteProperty(map, 'foo'));
assertTrue(delete proxyMap['foo']);

// get
assertSame(mapValue, map[mapKey]);
assertSame(mapValue, Reflect.get(map, mapKey));
assertSame(mapValue, proxyMap[mapKey]);

// set
map[mapKey] = newValue;
assertSame(newValue, map[mapKey]);
map[mapKey] = mapValue;

Reflect.set(map, mapKey, newValue);
assertSame(newValue, map[mapKey]);
map[mapKey] = mapValue;

proxyMap[mapKey] = newValue;
assertSame(newValue, proxyMap[mapKey]);
assertSame(newValue, map[mapKey]);
map[mapKey] = mapValue;
