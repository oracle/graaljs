/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import * as export_name from "./fixtures/export_name.mjs";

function assert(expected, actual) {
    if (expected !== actual) {
        var error = 'Values are not identical - '
                + 'expected: [' + expected + '] vs. '
                + 'actual: [' + actual +']';
        throw new Error(error);
    }
}

assert(1, export_name.one);
assert(2, export_name.onePlusOne);
assert(3, export_name.onePlusTwo);
assert(4, export_name.onePlusThree);

export {
    one,
    'onePlusOne',
    "onePlusTwo",
    onePlusOne as zeroPlusOnePlusOne,
    onePlusOne as 'onePlusZeroPlusOne',
    onePlusOne as "onePlusOnePlusZero",
    'onePlusTwo' as zeroPlusOnePlusTwo,
    'onePlusTwo' as 'onePlusZeroPlusTwo',
    'onePlusTwo' as "onePlusTwoPlusZero",
    "onePlusThree" as zeroPlusOnePlusThree,
    "onePlusThree" as 'onePlusZeroPlusThree',
    "onePlusThree" as "onePlusThreePlusZero",
} from "./fixtures/export_name.mjs";

export * as plainReExport from "./fixtures/export_name.mjs";
export * as 'quotedReExport' from "./fixtures/export_name.mjs";
export * as "doubleQuotedReExport" from "./fixtures/export_name.mjs";
