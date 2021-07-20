/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * WebAssembly.Table tests
 *
 * @option webassembly
 */

load('../js/assert.js')

// Missing table descriptor
assertThrows(() => {
    let table = new WebAssembly.Table();
}, TypeError);

// Empty table descriptor
assertThrows(() => {
    let table = new WebAssembly.Table({});
}, TypeError);

// Missing element in table descriptor
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 2});
}, TypeError);

// Missing initial in table descriptor
assertThrows(() => {
    let table = new WebAssembly.Table({element: 'anyfunc'});
}, TypeError);

// Table descriptor element is not 'anyfunc'
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 2, element: 'func'});
}, TypeError);

// Initial exceeds max table size
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 10000001, element: 'anyfunc'});
}, RangeError);

// Initial < maximum
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 5, maximum: 2, element: 'anyfunc'});
}, RangeError);

// Maximum exceeds max table size
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 2, maximum: 10000001, element: 'anyfunc'});
}, RangeError)

// Table length is initial length
{
    let table = new WebAssembly.Table({initial: 2, element: 'anyfunc'});
    assertSame(2, table.length)
}

// Grow beyond maximum
assertThrows(() => {
    let table = new WebAssembly.Table({initial: 2, maximum: 2, element: 'anyfunc'});
    table.grow(1);
}, RangeError)

// Length correct after grow
{
    let table = new WebAssembly.Table({initial: 2, maximum: 4, element: 'anyfunc'});
    assertSame(2, table.length);
    assertSame(2, table.grow(1));
    assertSame(3, table.length);
}