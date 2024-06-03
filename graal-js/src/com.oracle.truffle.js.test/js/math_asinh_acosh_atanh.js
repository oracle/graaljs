/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function assertEquals(msg, expected, actual, delta = 0) {
    if (Object.is(actual, expected)) {
        return;
    } else if (delta !== 0 && Math.abs(actual - expected) <= delta) {
        return;
    }
    throw new Error(`${msg} = actual [${actual}], expected [${expected}]${delta !== 0 ? "\u00b1" + delta : ""}`);
}

function asinhTest() {
    for (const [x, r, delta = 0] of [
            [1e-15, 1e-15], // GR-54085
            [3.7252902984619136e-9, 3.7252902984619136e-9],
            [2**-28, 2**-28],
            [3.725290298461915e-9, 3.725290298461915e-9],
            [1e-5, 9.999999999833334e-6],
            [0.5, 0.48121182505960347],
            [0.9999999999999999, 0.8813735870195429],
            [1.0, 0.881373587019543],
            [1.0000000000000002, 0.8813735870195432],
            [1.5, 1.1947632172871094],
            [1.9999999999999996, 1.44363547517881],
            [1.9999999999999998, 1.4436354751788103],
            [2.0, 1.4436354751788103],
            [2.0000000000000004, 1.4436354751788105],
            [5.0, 2.3124383412727525],
            [1e5, 12.206072645555174],
            [Math.PI, 1.8622957433108482],
            [11.548739357257748, Math.PI],
            [267.74489404101644, 2 * Math.PI],
            [2**28, 20.101268236238415],
            [268435456.00000006, 20.101268236238415], // GR-54495
            [2**29, 20.79441541679836],
            [2**30, 21.487562597358302],
            [1e15, 35.23192357547063],
            [1e300, 691.4686750787736],
            [2.2250738585072014e-308, 2.2250738585072014e-308], // GR-54085
            [5.244519529722009e+307, 709.2439543619927], // GR-54085
            [1e308, 709.889355822726],
            [Number.MAX_VALUE, 710.4758600739439],
            [Number.MIN_VALUE, Number.MIN_VALUE],
            [Number.POSITIVE_INFINITY, Number.POSITIVE_INFINITY],
            [Number.NaN, Number.NaN],
        ]) {
        assertEquals("asinh(" + x + ")", r, Math.asinh(x), delta);
        assertEquals("asinh(" + -x + ")", -r, Math.asinh(-x), delta);
    }
}

function acoshTest() {
    for (const [x, r, delta = 0] of [
            // x < 1 := NaN
            [0.9999999999999999, Number.NaN],
            [2**-28, Number.NaN],
            [1e-15, Number.NaN],
            [Number.MIN_VALUE, Number.MIN_VALUE],
            // x >= 1
            [1.0, 0.0],
            [1.0000000000000002, 2.1073424255447017e-8],
            [1.1, 0.4435682543851154],
            [1.5, 0.9624236501192069],
            [2.0, 1.3169578969248166],
            [5.0, 2.2924316695611777],
            [1e5, 12.206072645505174],
            [Math.PI, 1.811526272460853],
            [11.591953275521519, Math.PI],
            [267.7467614837482, 2 * Math.PI],
            [2**28, 20.10126823623841],
            [268435456.0000002, 20.10126823623841],
            [268435456.00000024, 20.101268236238415],
            [2**29, 20.79441541679836],
            [2**30, 21.487562597358302],
            [1e15, 35.23192357547063],
            [1.5243074119957227e+267, 615.904907160801], // GR-54085 (127 ** 127)
            [1e300, 691.4686750787736],
            [2.2250738585072014e-308, 2.2250738585072014e-308],
            [5.244519529722009e+307, 709.2439543619927],
            [1e308, 709.889355822726],
            [Number.MAX_VALUE, 710.4758600739439],
            [Number.POSITIVE_INFINITY, Number.POSITIVE_INFINITY],
            [Number.NaN, Number.NaN],
        ]) {
        assertEquals("acosh(" + x + ")", x >= 1 ? r : Number.NaN, Math.acosh(x), delta);
        assertEquals("acosh(" + -x + ")", Number.NaN, Math.acosh(-x), delta);
    }
}

function atanhTest() {
    for (const [x, r, delta = 0] of [
            // |x| > 1 := NaN
            [1.0000000000000002, Number.NaN],
            [Number.POSITIVE_INFINITY, Number.NaN],
            // |x| == 1 := +/-inf
            [1.0, Number.POSITIVE_INFINITY],
            // |x| < 1
            [0.9999999999999999, 18.714973875118524],
            [0.9999930253396107, 6.283185307182609],
            [0.99627207622075, 3.141592653589798],
            [0.9, 1.4722194895832204],
            [0.8, 1.0986122886681098],
            [0.7, 0.8673005276940531],
            [0.6, 0.6931471805599453],
            [0.5, 0.5493061443340548],
            [0.4, 0.42364893019360184],
            [0.3, 0.30951960420311175],
            [0.2, 0.2027325540540822],
            [0.1, 0.10033534773107558],
            [1e-5, 1.0000000000333335e-5],
            [1e-15, 1e-15], // GR-54085
            [0.0, 0.0],
            [3.7252902984619136e-9, 3.7252902984619136e-9],
            [2**-28, 2**-28],
            [3.725290298461915e-9, 3.725290298461915e-9],
            [2**-29, 2**-29],
            [Number.MIN_VALUE, Number.MIN_VALUE],
            [Number.NaN, Number.NaN],
        ]) {
        assertEquals("atanh(" + x + ")", r, Math.atanh(x), delta);
        assertEquals("atanh(" + -x + ")", -r, Math.atanh(-x), delta);
    }
}

asinhTest();
acoshTest();
atanhTest();
