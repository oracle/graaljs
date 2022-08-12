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

var hits = [];

function left() { hits.push('L I'); }
function right() { hits.push('R I'); }


//---------------------------------------------//
@(hits.push('L E'), left) @(hits.push('R E'), right) class C1 {}

assertSameContent(['L E', 'R E', 'R I', 'L I'], hits);


//---------------------------------------------//
hits = [];

class C2 { @(hits.push('L E'), left) @(hits.push('R E'), right) m() {} }

assertSameContent(['L E', 'R E', 'R I', 'L I'], hits);


//---------------------------------------------//
hits = [];

assertThrows(() => {
    function dec() {}
    @(hits.push('left'), dec) @(42) @(hits.push('right'), dec) class C {}
}, TypeError)
assertSameContent(['left', 'right'], hits);
