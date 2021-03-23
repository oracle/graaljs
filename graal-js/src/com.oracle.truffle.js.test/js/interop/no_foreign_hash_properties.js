/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option foreign-hash-properties=false
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let foreignMap = new HashMap();
foreignMap.put("key1", "value1");
foreignMap.put("key2", "value2");

assertSame(undefined, foreignMap.key1);
assertSame(undefined, foreignMap["key2"]);
