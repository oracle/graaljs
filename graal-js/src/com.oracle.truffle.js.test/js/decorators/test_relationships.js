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

const CHILDREN = new WeakMap();

function registerChild(parent, child) {
    let children = CHILDREN.get(parent);
    if (children === undefined) {
        children = [];
        CHILDREN.set(parent, children);
    }
    children.push(child);
}

function getChildren(parent) {
    return CHILDREN.get(parent);
}

function register() {
    return function(value) {
        registerChild(this, value);
        return value;
    }
}

class Child { toString() {return 'Child'} }
class OtherChild { toString() {return 'OtherChild'} }

class Parent {
    @register child1 = new Child();
    @register child2 = new OtherChild();
}

let parent = new Parent();

let children = getChildren(parent);
assertSame(2, children.length)
assertSame('Child', children[0].toString());
assertSame('OtherChild', children[1].toString());
