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

var tick = 39;

function f(value, context) {
    assertSame('#m', context.name);
    tick++;
}

var C1 = class { @f static #m() {} }

var C2 = class { @f static get #m() {} }

var C3 = class { @f static set #m(value) {} }

assertSame(42, tick);