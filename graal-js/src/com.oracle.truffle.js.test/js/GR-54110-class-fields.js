/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Ensure class fields are correctly defined using CreateDataPropertyOrThrow semantics.
 */

load("assert.js");

class X {
    field = Object.defineProperty(
        this,
        "field2",
        { writable: true, configurable: false, value: 1 }
    );
    ['field2'] = true;
}
assertThrows(() => new X(), TypeError);

let setterCalled = false;
let Y = class Y {
  field = (() => {
    Object.defineProperty(
      this,
      "setterField",
      { configurable: true, set(val) { setterCalled = true; } }
    );
    return 1;
  })();
  setterField = "written";
};
let y = new Y();
assertFalse(setterCalled);
assertSame("written", y.setterField);

class ProxyBase {
    static trapCalls;
    constructor(){
        ProxyBase.trapCalls = [];
        return new Proxy({}, {
            get(target, key) {
                return target[key];
            },
            defineProperty(target, key, desc) {
                ProxyBase.trapCalls.push(key);
                Object.defineProperty(target, key, desc);
                return target;
            }
        });
    }
}
class Z extends ProxyBase {
    field = (() => {
        Object.defineProperty(
        this,
        "setterField",
        { configurable: true, set(val) { setterCalled = true; } }
        );
        return 1;
    })();
    setterField = "written";
};
let z = new Z();
assertFalse(setterCalled);
assertSame("written", y.setterField);
assertSameContent(["setterField", "field", "setterField"], ProxyBase.trapCalls);
