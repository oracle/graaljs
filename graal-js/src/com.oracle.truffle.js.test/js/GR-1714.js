/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test ensuring variables not captured by closures are not kept alive.
 *
 * @option js.scope-optimization=true
 */

const largeSize = 1e7;
const limit = 1000;

let alloc = function() {
    // Allocate large temporary data not referenced by any closure.
    let tempData = new Uint8Array(largeSize);
    let holyGrail = tempData[0] + tempData[tempData.length - 1];

    // holyGrail is used by the function that is returned, but the temporary data are NOT used.
    let result = function(x) {
        return Math.abs(x - holyGrail);
    };
    return result;
};

// This may take a couple of seconds to run but it should not run out of memory.
let cache = [];
for (let i = 0; i < limit; i++) {
    cache[i] = alloc();
}
for (let i = 0; i < limit; i++) {
    cache[i]();
}
