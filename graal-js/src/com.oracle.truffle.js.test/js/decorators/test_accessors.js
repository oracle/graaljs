/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')


//---------------------------------------------//
class C1 {
  accessor
  $;
  static accessor
  $;
}
let c1 = new C1();

assertSame(false, Object.hasOwnProperty.call(C1.prototype, 'accessor'));
assertSame(false, Object.hasOwnProperty.call(C1.prototype, '$'));

assertSame(true, Object.hasOwnProperty.call(C1, 'accessor'));
assertSame(false, Object.hasOwnProperty.call(C1, '$'));

assertSame(true, Object.hasOwnProperty.call(c1, 'accessor'));
assertSame(true, Object.hasOwnProperty.call(c1, '$'));


//---------------------------------------------//
class C2 {
  accessor x = 42;
}
let c2 = new C2();
assertSame(42, c2.x);
c2.x = 123;
assertSame(123, c2.x);


//---------------------------------------------//
class C3 {
   static accessor x = 42;
}
assertSame(42, C3.x);
C3.x = 123;
assertSame(123, C3.x);


//---------------------------------------------//
class C4 { static accessor }
C4.accessor = 42;
assertSame(42, C4.accessor)


//---------------------------------------------//
class C5 { static accessor = 42; }
assertSame(42, C5.accessor)


//---------------------------------------------//
assertThrows(() => {
  Object.getOwnPropertyDescriptor(class { static accessor x }, 'x').get.call({})
}, TypeError);
assertThrows(() => {
  Object.getOwnPropertyDescriptor(class { static accessor x }, 'x').set.call({})
}, TypeError);


//---------------------------------------------//
const magic = 42;
var C6 = class { static accessor [magic]; }
assertSame('object', typeof Object.getOwnPropertyDescriptor(C6, '42'));
assertSame(undefined, Object.getOwnPropertyDescriptor(C6, 'magic'))


//---------------------------------------------//
class C7 { static accessor x; }

assertThrows(() => {
  Object.getOwnPropertyDescriptor(C7, 'x').get.call(Object.create(C7)) // should throw
});
assertThrows(() => {
  Object.getOwnPropertyDescriptor(C7, 'x').set.call(Object.create(C7)) // should throw
});


//---------------------------------------------//
var C8 = class { static accessor x = 42 }
assertSame('function', typeof Object.getOwnPropertyDescriptor(C8, 'x').get);
assertSame(42, C8.x);


//---------------------------------------------//
var symbol = Symbol('foo');
var C9 = class { static accessor [symbol] = 32 }

assertSame('function', typeof Object.getOwnPropertyDescriptor(C9, symbol).get);
assertSame(32, C9[symbol]);
