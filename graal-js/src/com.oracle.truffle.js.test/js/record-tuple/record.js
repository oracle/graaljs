/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=2022
 */

load('../assert.js');
let a, b, c;

/*
 * Test 1:
 * valid records
 */
a = #{};
b = #{ a: 1, b: 2 };
c = #{ a: 1, b: #[2, 3, #{ c: 4 }] };

/*
 * Test 2:
 * Non-String key
 */
assertThrows(function() {
    eval("const x = #{ [Symbol()]: #{} }"); // TypeError: Record may only have string as keys
}, TypeError);

/*
 * Test 3:
 * concise methods
 */
assertThrows(function() {
    eval("#{ method() { } }");  // SyntaxError, concise methods are disallowed in Record syntax
}, SyntaxError);

/*
 * Test 4:
 * __proto__
 */
const y = #{ "__proto__": 1 }; // valid, creates a record with a "__proto__" property.
assertThrows(function() {
    eval("const x = #{ __proto__: 1 }"); // SyntaxError, __proto__ identifier prevented by syntax
}, SyntaxError);

/*
 * Test 5:
 * nested records
 */
a = #{ id: 1, child: #{ grandchild: #{ result: 42 } } };
assertSame(1, a.id);
assertSame(42, a.child.grandchild.result);

/*
 * Test 6:
 * [[OwnPropertyKeys]]
 */
a = #{ id: 1, data: "Hello World!" };
assertSame(["data", "id"].toString(), Object.getOwnPropertyNames(a).toString());

/*
 * Test 7:
 * [[GetOwnProperty]]
 */
a = #{ age: 22 };
b = Object.getOwnPropertyDescriptors(a);
assertSame(22, b.age.value);
assertSame(false, b.age.writable);
assertSame(true, b.age.enumerable);
assertSame(false, b.age.configurable);

/*
 * Test 8:
 * [[DefineOwnProperty]]
 */
a = #{ age: 22 };
a = Object(a);
Object.defineProperty(a, "age", { value: 22 });
Object.defineProperty(a, "age", { value: 22, writable: false, enumerable: true, configurable: false });
assertThrows(function() {
    eval(`Object.defineProperty(a, "age", { value: 21 });`); // value must be the same
}, TypeError);
assertThrows(function() {
    eval(`Object.defineProperty(a, "age", { writable: false, enumerable: true, configurable: false });`); // value must be provided
}, TypeError);
assertThrows(function() {
    eval(`Object.defineProperty(a, "age", { value: 22, writable: true });`); // writeable must always be false
}, TypeError);
assertThrows(function() {
    eval(`Object.defineProperty(a, "age", { value: 22, enumerable: false });`); // enumerable must always be true
}, TypeError);
assertThrows(function() {
    eval(`Object.defineProperty(a, "age", { value: 22, configurable: true });`); // configurable must always be false
}, TypeError);
assertThrows(function() {
    eval(`Object.defineProperty(a, Symbol("42"), { value: 22 });`); // Symbols are not allowed
}, TypeError);

/*
 * Test 9:
 * [[Delete]]
 */
a = #{ age: 22 };
assertSame(false, delete a.age);
assertSame(true, delete a.unknown);
assertSame(true, delete a[Symbol("test")]);

/*
 * Test 10:
 * Type Conversion
 */
a = #{ age: 22 };
// 3.1.1 ToBoolean
assertSame(true, !!a); // JSToBigIntNode
if (a) { // JSToBooleanUnaryNode
    // ok
} else {
    fail("#{ age: 22 } should be true")
}
// 3.1.2 ToNumber
assertThrows(function() {
    eval("a + 1"); // JSToNumberNode
}, TypeError);
assertThrows(function() {
    eval("Math.abs(a)"); // JSToDoubleNode
}, TypeError);
assertThrows(function() {
    eval("parseInt(\"1\", a)"); // JSToInt32Node
}, TypeError);
assertThrows(function() {
    eval("'ABC'.codePointAt(a)"); // JSToIntegerAsIntNode
}, TypeError);
assertThrows(function() {
    eval("[1].at(a)"); // JSToIntegerAsLongNode
}, TypeError);
assertThrows(function() {
    eval("'ABC'.split('', a);"); // JSToUInt32Node
}, TypeError);
// 3.1.3 ToBigInt
assertThrows(function() {
    eval("BigInt.asIntN(64, a)");
}, TypeError);
// 3.1.4 ToString
assertSame("[object Record]", a + ""); // JSToStringNode
assertSame(false, a < #{}); // JSToStringOrNumberNode
assertSame(true, a <= #{});
assertSame(true, a >= #{});
assertSame(false, a > #{});
// 3.1.5 ToObject
assertSame("object", typeof Object(a)); // JSToObjectNode

/*
 * Test 11:
 * typeof
 */
assertSame("record", typeof #{id: 1});
assertSame("record", typeof #{});
