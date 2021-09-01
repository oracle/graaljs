/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import { other } from "test_import_binding_other.mjs";

const kTestString = 'hello';
globalThis.run = cb => cb();

// true branch has block scope, false branch has not
if (false) {
    const bla = "bla";
    run(() => bla);
} else {
    run(() => {
        run(() => {
                other(kTestString);
            });
        });
}
