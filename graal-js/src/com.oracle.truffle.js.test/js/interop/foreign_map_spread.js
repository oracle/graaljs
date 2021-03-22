/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let foreignMap = new HashMap();
foreignMap.put("key1", "value1");
foreignMap.put("key2", "value2");
foreignMap.put("key3", "value3");
foreignMap.put(13.37, 3.14);

let spread = {...foreignMap};
assertSame("value1", spread.key1);
assertSame("value2", spread.key2);
assertSame("value3", spread.key3);

let {key1, ...rest} = foreignMap;
assertSame(undefined, rest.key1);
assertSame("value2", rest.key2);
assertSame("value3", rest.key3);
