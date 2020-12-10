/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/**
 * Test for the relative-indexing-method proposal.
 * https://tc39.es/proposal-relative-indexing-method/
 * 
 * @option ecmascript-version=2022
 */

load('../assert.js');

function arrayPrototypeTest() {
  var arr = [0,1,2,3,4,5];

  //in bound
  assertSame(0, arr.at(0));
  assertSame(1, arr.at(1));
  assertSame(5, arr.at(5));

  //out of bound
  assertSame(undefined, arr.at(6));
  assertSame(5, arr.at(-1));
  assertSame(4, arr.at(-2));
  
  //general
  assertSame(1, Array.prototype.at.length);
}

function stringPrototypeTest() {
  var str = "Graal.js";

  //in bound
  assertSame("G", str.at(0));
  assertSame("r", str.at(1));
  assertSame("s", str.at(7));

  //out of bound
  assertSame(undefined, str.at(100));
  assertSame("s", str.at(-1));
  assertSame("j", str.at(-2));

  //general
  assertSame(1, String.prototype.at.length);
}

function typedArrayPrototypeTestIntl(constr) {
  var arr = new constr([0,1,2,3,4,5]);

  //in bound
  assertSame(0, arr.at(0));
  assertSame(1, arr.at(1));
  assertSame(5, arr.at(5));

  //out of bound
  assertSame(undefined, arr.at(100));
  assertSame(5, arr.at(-1));
  assertSame(4, arr.at(-2));

  //general
  assertSame(1, constr.prototype.at.length);
}

function typedArrayPrototypeTest() {
  var types = [Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array,
               Uint32Array, Float32Array, Float64Array];
  types.forEach( constr => {
    typedArrayPrototypeTestIntl(constr);
  });
}

arrayPrototypeTest();
stringPrototypeTest();
typedArrayPrototypeTest();
