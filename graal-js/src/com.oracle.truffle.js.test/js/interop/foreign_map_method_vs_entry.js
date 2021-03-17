/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let functionValue = () => {fail("should not be called");}

let foreignMap = new HashMap();
foreignMap.put("size", 42);
foreignMap.put("clear", functionValue);

assertSame(42, foreignMap.size);
assertSame(2, foreignMap.size());

assertSame(functionValue, foreignMap.clear);
foreignMap.clear();
assertSame(0, foreignMap.size());
