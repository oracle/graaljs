/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of longer lists by Intl.ListFormat.
 */

load("assert.js");

const length = 300;
const result = new Intl.ListFormat('en').formatToParts(new Array(300).fill('x'));
assertSame(2*length-1, result.length);
for (let i = 0; i < result.length; i++) {
    const part = result[i];
    if (i%2 === 0) {
        assertSame('element', part.type);
        assertSame('x', part.value);
    } else {
        assertSame('literal', part.type);
        const expectedLiteral = (i === result.length - 2) ? ', and ' : ', ';
        assertSame(expectedLiteral, part.value);
    }
}

// Original test-case from the fuzzer,
// just make sure that we do not throw an internal error

const v2 = "{3991}, {3992}, {3993}, {3994}, {3995}, {3996}, {3997}, {3998}, {3999}, {4000}, {4001}, {4002}, {4003}, {4004}, {4005}, {4006}, {4007}, {4008}, {4009}, {4010}, {4011}, {4012}, {4013}, {4014}, {4015}, {4016}, {4017}, {4018}, {4019}, {4020}, {4021}, and {4022}";
const v7 = new Intl.ListFormat();
v7.formatToParts(v2);