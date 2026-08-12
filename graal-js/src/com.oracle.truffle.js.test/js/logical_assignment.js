/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of logical assignment operators.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

var return0 = function() {
    return 0;
};
var return1 = function() {
    return 1;
};
var thrower = function() {
    throw new Error('Unexpected invocation of setter.');
};

var obj = {};
Object.defineProperty(obj, "prop0", { get: return0, set: thrower });
Object.defineProperty(obj, "prop1", { get: return1, set: thrower });

assertSame(0, obj['prop0'] &&= 42);
assertSame(1, obj['prop1'] ||= 42);
assertSame(1, obj['prop1'] ??= 42);

with (obj) {
  assertSame(0, prop0 &&= 42);
  assertSame(1, prop1 ||= 42);
  assertSame(1, prop1 ??= 42);
}

Object.defineProperty(this, "prop0", { get: return0, set: thrower });
Object.defineProperty(this, "prop1", { get: return1, set: thrower });

assertSame(0, prop0 &&= 42);
assertSame(1, prop1 ||= 42);
assertSame(1, prop1 ??= 42);

function testComputedPropertyKey(operator, initialValue, assignedValue) {
    var conversionCount = 0;
    var key = {
        [Symbol.toPrimitive]: function(hint) {
            assertSame('string', hint);
            conversionCount++;
            return 'property';
        }
    };
    var target = {property: initialValue};

    operator(target, key, assignedValue);

    assertSame(assignedValue, target.property);
    assertSame(1, conversionCount);
}

testComputedPropertyKey((target, key, value) => target[key] &&= value, true, false);
testComputedPropertyKey((target, key, value) => target[key] ||= value, false, true);
testComputedPropertyKey((target, key, value) => target[key] ??= value, null, true);

function testLogicalAssignmentRetainsTarget() {
    var replacement = {};
    var target = {property: true};
    var original = target;
    assertSame(42, target['property'] &&= (target = replacement, 42));
    assertSame(42, original.property);
    assertFalse('property' in replacement);

    target = {property: false};
    original = target;
    assertSame(42, target['property'] ||= (target = replacement, 42));
    assertSame(42, original.property);
    assertFalse('property' in replacement);

    target = {property: null};
    original = target;
    assertSame(42, target['property'] ??= (target = replacement, 42));
    assertSame(42, original.property);
    assertFalse('property' in replacement);
}

testLogicalAssignmentRetainsTarget();

var conversionCount = 0;
var key = {
    [Symbol.toPrimitive]: function() {
        conversionCount++;
        return 'property';
    }
};
assertThrows(() => null[key] ||= true, TypeError);
assertSame(0, conversionCount);

conversionCount = 0;
class Base {
    get property() {
        return this.value;
    }

    set property(value) {
        this.value = value;
    }
}
class Derived extends Base {
    assign(key) {
        return super[key] ||= 42;
    }
}
var derived = new Derived();
derived.value = 0;
assertSame(42, derived.assign(key));
assertSame(42, derived.value);
assertSame(1, conversionCount);
