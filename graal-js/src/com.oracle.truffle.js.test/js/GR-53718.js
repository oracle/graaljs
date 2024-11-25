/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js")

function testConstructArrayOneForeignObjectArg() {
    const v0 = [];
    new Array(this.Java.to(v0).clone.call());
    new Array();
}

function testCanonicalizeLocaleListShortValue() {
    const v2 = [8112n];
    const v5 = this.Java.to(v2);
    assertThrows(() => {(238).__proto__.toLocaleString(v5);}, TypeError); // String or Object expected in locales list, got number
}

function testLong1() {
    const v1 = [2147483649n];
    const v2 = this.Java.to(v1);
    new Int16Array(v2);
    new Int32Array(v2);
}

function testLong2() {
    const v0 = [];
    const v3 = [-2147483649n];
    v0["toSpliced"](this.Java.to(v3).find(eval));
}

function testLong3() {
    const v2 = [-2147483649n];
    const v6 = this.Java.to(v2).find(Symbol);
    class C7 {
    }

    const v8 = new C7();
    function f9(a10, a11) {
    const v14 = ("35922").constructor.fromCharCode();
    v14.substring(v14).codePointAt(v6);
    return v8;
    }

    const v17 = new C7();
    v17 <= v8;
    Object.defineProperty(v8, "toString", { configurable: true, enumerable: true, value: f9 });
    assertThrows(() => v17 <= v8, TypeError); // Cannot convert object to primitive value
}

testConstructArrayOneForeignObjectArg();
testCanonicalizeLocaleListShortValue();
testLong1();
testLong2();
testLong3();
