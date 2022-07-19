/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that the class name binding is not accessible in the extends clause.
 */

load('assert.js')

// Should throw ReferenceError: Cannot access 'x' before initialization
assertThrows(() => { class x extends x {}; }, ReferenceError);
assertThrows(() => { (class x extends x {}); }, ReferenceError);
assertThrows(() => { var x = class x extends x {}; }, ReferenceError);

assertThrows(() => { class x extends (() => x)() {}; }, ReferenceError);
assertThrows(() => { (class x extends (() => x)() {}); }, ReferenceError);
assertThrows(() => { var x = class x extends (() => x)() {}; }, ReferenceError);

assertThrows(() => { class x { [x]() {} }; }, ReferenceError);
assertThrows(() => { (class x { [x]() {} }); }, ReferenceError);
assertThrows(() => { var x = class x { [x]() {} }; }, ReferenceError);

assertThrows(() => { class x { [(() => x)()]() {} }; }, ReferenceError);
assertThrows(() => { (class x { [(() => x)()]() {} }); }, ReferenceError);
assertThrows(() => { var x = class x { [(() => x)()]() {} }; }, ReferenceError);
