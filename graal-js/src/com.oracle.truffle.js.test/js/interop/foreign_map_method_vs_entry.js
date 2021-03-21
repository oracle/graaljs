/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests foreign map method vs. hash entry precedence.
 *
 * @option foreign-object-prototype
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let doNotCallMe = () => {fail("should not be called");};
let callMeMaybe = () => 'ok';

let foreignMap = new HashMap();
foreignMap.put("size", 42);
foreignMap.put("clear", doNotCallMe);
foreignMap.put("callMeMaybe", callMeMaybe);
foreignMap.put("set", doNotCallMe);
foreignMap.put("notCallable", 42);

// invocation favors methods over hash entries.
assertSame(42, foreignMap.size);
assertSame(5, foreignMap.size());

// invocation favors foreign object prototype methods over hash entries, too.
foreignMap.set("set", callMeMaybe);
assertSame(callMeMaybe, foreignMap.set);

assertThrows(() => foreignMap.doesNotExist(), TypeError);
assertThrows(() => foreignMap.notCallable(), TypeError);

// if the hash entry is callable and there's no member with the same name, we should be able to invoke it.
assertSame('ok', foreignMap.callMeMaybe());

assertSame(doNotCallMe, foreignMap.clear);
foreignMap.clear();
assertSame(0, foreignMap.size());
