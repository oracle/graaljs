/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

for (let d = 0; d < 1; d++) {
    with ({}) with ({}) eval("this");
}

assertSame(42,
    (function () {
        for (let d = 0; d < 1; d++) {
            with ({x: 13}) {
                with ({y: 14}) {
                    with ({z: 15}) {
                        return eval("x + y + z");
                    }
                }
            }
        }
    })()
);
