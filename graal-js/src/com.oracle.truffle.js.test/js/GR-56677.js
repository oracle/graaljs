/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for ArrayIndexOutOfBoundsException in RegExp flags getter with 8 flags set.
 */

load("assert.js");

const re = /[^123]+/digymsv;
// u and v flags are mutually exclusive, but we can fake one of the two.
Object.defineProperty(re, "unicode", { value: true, configurable: true, writable: true });
assertSame("dgimsuvy", re.flags);

// Original test case from fuzz testing
const v1 = /[^123]+/digymsv;
Object.defineProperty(v1, "unicode", { enumerable: true, value: "-12" });
v1.test(v1);
