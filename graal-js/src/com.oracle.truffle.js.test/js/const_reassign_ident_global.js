/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests correct immutable binding reassignment handling for `const` variables in the global scope
 * via increment (++), decrement (--), and compound assignment operators.
 */

const i = 4; try { i++; fail(); } catch (e) { if (!(e instanceof TypeError)) throw e; }
const j = 4; try { ++j; fail(); } catch (e) { if (!(e instanceof TypeError)) throw e; }
const k = 4; try { k--; fail(); } catch (e) { if (!(e instanceof TypeError)) throw e; }
const l = 4; try { --l; fail(); } catch (e) { if (!(e instanceof TypeError)) throw e; }
    
// If binding is immutable, compound assignment should fail only after the binary operator (including both its operands) has been executed.
let side_effects = 0;
const m = {toString() { side_effects += 1; return "a"; }};
try {
    m += {toString() { side_effects += 2; return "b"; }};
    fail();
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}
if (side_effects !== 3) throw new Error(`${side_effects}`);

// TypeError due to mixing BigInt and Number is thrown first.
const n = 1n;
try {
    n += 1;
    fail();
} catch (e) {
    if (!(e instanceof TypeError && e.message.includes("BigInt"))) throw e;
}

function fail() {
    throw new Error("Expected a TypeError, but no error was thrown.");
}
