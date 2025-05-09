/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests of Upsert proposal.
 * 
 * @option ecmascript-version=staging
 */

load('assert.js');

// getOrInsert should work for foreign maps 
var javaMap = new java.util.HashMap();
assertSame(null, javaMap.get('aKey'));
assertSame('aValue', javaMap.getOrInsert('aKey', 'aValue'));
assertSame('aValue', javaMap.getOrInsert('aKey', 'aValue2'));
assertSame('aValue', javaMap.get('aKey'));

// getOrInsertComputed should work for foreign maps
var callback = (key) => 'value-for-' + key;
assertSame('aValue', javaMap.getOrInsertComputed('aKey', callback));
assertSame('value-for-aKey2', javaMap.getOrInsertComputed('aKey2', callback));
assertSame('value-for-aKey2', javaMap.get('aKey2'));
