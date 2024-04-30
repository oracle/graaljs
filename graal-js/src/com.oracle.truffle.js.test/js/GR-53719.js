/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function f0(a1, a2, a3) {
    return f0;
}
const v4 = f0.apply;
function f5(a6) {
    let v7;
    try {
        v7 = a6.apply();
    } catch (e) {}
    const v8 = [1.0596327698438966e+308, 0.39734779720274815];
    function f9(a10, a11, a12, a13) {
        return v8 instanceof a12;
    }
    try {
        f9(v7, v7, v7);
    } catch {}
    return f5;
}
-65535 instanceof v4.bind(f5(f5));
