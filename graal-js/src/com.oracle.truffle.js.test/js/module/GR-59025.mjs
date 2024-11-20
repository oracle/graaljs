/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the usage of for-await-of in the body of a module.
 */

let sum = 0;
for await (let x of [42, 211]) sum += x;
if (sum !== 42 + 211) {
   throw new Error("Sum of 24 and 211 is not " + sum);
}
