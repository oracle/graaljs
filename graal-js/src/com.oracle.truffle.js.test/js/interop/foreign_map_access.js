/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let obj = {};

let foreignMap = new HashMap();
foreignMap.put("key", "value");
foreignMap.put(obj, 42);
foreignMap.put(13.37, 3.14);

assertSame("value", foreignMap["key"]);
assertSame("value", foreignMap.key);
assertSame(42, foreignMap[obj]);
assertSame(3.14, foreignMap[13.37]);
assertSame(undefined, foreignMap['unknown']);
assertSame(undefined, foreignMap.unknown);

foreignMap["key2"] = "value2"
foreignMap.key3 = "value3"
assertSame("value2", foreignMap.key2);
assertSame("value3", foreignMap["key" + "3"]);
