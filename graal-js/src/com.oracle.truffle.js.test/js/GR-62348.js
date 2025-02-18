/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test calling RegExp.prototype[Symbol.replace] with a foreign/host replacer function.
 */

load("./assert.js");

function F1(replacer) {
    return ("includes").replace(/((((.).).).)/vim, replacer);
}
function F2(replacer) {
    return ("includes").replace(/(?<a>(?<b>(?<c>(?<d>.).).).)/vim, replacer);
}
function F3(replacer) {
    return ("includes").replaceAll(/((((.).).).)/g, replacer);
}
function F4(replacer) {
    return ("includes").replace("lu", replacer);
}
function F5(replacer) {
    return ("includes").replaceAll("lu", replacer);
}

const v1 = Java.type("java.util.List").of;

assertSame('[incl, incl, inc, in, i, 0, includes]udes', F1(v1));
assertSame('[incl, incl, inc, in, i, 0, includes, Object{a: "incl", b: "inc", c: "in", d: "i"}]udes', F2(v1));
assertSame('[incl, incl, inc, in, i, 0, includes][udes, udes, ude, ud, u, 4, includes]', F3(v1));
assertSame('inc[lu, 3, includes]des', F4(v1));
assertSame('inc[lu, 3, includes]des', F5(v1));
