/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify that assignment to a constant variable using logical assignment operators throws a TypeError.
 */

load("assert.js");

{ const C = undefined; C &&= 0; }
assertThrows(() => { const C = undefined; C ||= 0; }, TypeError);
assertThrows(() => { const C = undefined; C ??= 0; }, TypeError);

assertThrows(() => { class C { static x = C &&= 0 } }, TypeError);
assertThrows(() => { class C { static { C &&= 0; } } }, TypeError);
assertThrows(() => { new (class C { x = C &&= 0 })() }, TypeError);
assertThrows(() => { (class C { static m() { C &&= 0; } }).m(); }, TypeError);
assertThrows(() => { new (class C { m() { C &&= 0; } })().m(); }, TypeError);

// ||= and ??= do not throw due to short-circuiting semantics.
{ class C { static x = C ||= 0 } }
{ class C { static { C ||= 0; } } }
{ new (class C { x = C ||= 0 })() }
{ (class C { static m() { C ||= 0; } }).m(); }
{ new (class C { m() { C ||= 0; } })().m(); }

{ class C { static x = C ??= 0 } }
{ class C { static { C ??= 0; } } }
{ new (class C { x = C ??= 0 })() }
{ (class C { static m() { C ??= 0; } }).m(); }
{ new (class C { m() { C ??= 0; } })().m(); }
