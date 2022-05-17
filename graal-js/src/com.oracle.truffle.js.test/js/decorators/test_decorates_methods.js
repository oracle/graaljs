/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');


//---------------------------------------------//
var tick = 41;

function dec1(v, c) {
    assertSame('function', typeof v);
    assertSame('method', c.kind)
    assertSame('foo', c.name)
    assertSame(false, c.static)
    tick++;
}

class C1 {
    @dec1
    foo() { };
}

assertSame(42, tick);


//---------------------------------------------//
tick = 41;

function dec2(v, c) {
    assertSame('function', typeof v);
    assertSame('method', c.kind)
    assertSame('sfoo', c.name)
    assertSame(true, c.static)
    tick++;
}

class C2 {
    @dec2
    static sfoo() { };
}

assertSame(42, tick);


//---------------------------------------------//
tick = 41;

function justInit(value, { name }) {
    tick++;
}

class C3 {
    @justInit
    meth1() {
        throw 'should not call';
    }

    toString() {
        throw 'should not call';
    }
}

new C3();
assertSame(42, tick);


//---------------------------------------------//
var hits = [];

function initNew(value, { name }) {
    hits.push('initNew');
    hits.push(name);
    return function() {
        hits.push('return-42');
        return 42;
    };
}

class C4 {
    @initNew
    ['meth' + 1]() {
        throw "should not be called!";
    }

    toString() {
        return 'a string';
    }
}

let c = new C4();
assertSame('a string', c.toString());
assertSameContent(['initNew', 'meth1'], hits);
assertSame(42, c.meth1());
assertSameContent(['initNew', 'meth1', 'return-42'], hits);
assertSame('meth1', c.meth1.name);
