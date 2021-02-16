/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
load("assert.js");
var assertEquals = assertSame;

function assertProxyArray(arr, derived = true) {
  assertEquals(3, arr.length);
  assertEquals(42, arr[0]);
  assertEquals(43, arr[1]);
  assertEquals(44, arr[2]);
  assertEquals(derived, arr.hasOwnProperty('derived'));
}

class MyArray extends Array {
  constructor(...args) {
    super(...args);
    this.derived = true;
  }
}

class MyTypedArray extends Int8Array {
  constructor(...args) {
    super(...args);
    this.derived = true;
  }
}

(function() {
    let a = [42, 43, 44];
    a.constructor = new Proxy(MyArray, {});
    assertProxyArray(a.filter(a => true));
    assertProxyArray(a.map(a => a));
    assertProxyArray(a.slice());
})();

(function() {
    let a = new Int8Array([42, 43, 44]);
    a.constructor = new Proxy(MyTypedArray, {});
    assertProxyArray(a.filter(a => true));
    assertProxyArray(a.map(a => a));
    assertProxyArray(a.slice());
})();

(function() {
    let elements = [42, 43, 44];
    for (let [myCtor, baseCtor, args] of [[MyArray, Array, elements], [MyTypedArray, Int8Array, [elements]]]) {
        let get_prototype_count = 0;
        let Ctor = new Proxy(myCtor, {
          get(target, property, receiver) {
            if (property == "prototype") {
              get_prototype_count++;
            }
            return Reflect.get(target, property, receiver);
          }
        });
        let a = Reflect.construct(baseCtor, args, Ctor);
        assertEquals(1, get_prototype_count);
        assertProxyArray(a, false);
    }
})();

(function() {
    let elements = [42, 43, 44];
    for (let [myCtor, args] of [[MyArray, elements], [MyTypedArray, [elements]]]) {
        let get_prototype_count = 0;
        let Ctor = new Proxy(myCtor, {
          get(target, property, receiver) {
            if (property == "prototype") {
              get_prototype_count++;
            }
            return Reflect.get(target, property, receiver);
          }
        });
        let a = new Ctor(...args);
        a.constructor = Ctor;
        assertEquals(1, get_prototype_count);
        assertProxyArray(a.filter(a => true));
        assertEquals(2, get_prototype_count);
        assertProxyArray(a.map(a => a));
        assertEquals(3, get_prototype_count);
        assertProxyArray(a.slice());
        assertEquals(4, get_prototype_count);
    }
})();
