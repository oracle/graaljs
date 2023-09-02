/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/**
 * Test for Array.prototype.toReversed (Proposal Change Array by copy).
 * Note: there are more tests in ArrayPrototypeBuiltinsTest.
 *
 * @option ecmascript-version=staging
 */
load("assert.js");

assertSameContent(["a", "b", "c", "d"], Array.prototype.toReversed.call("dcba"));
assertSameContent(["g", "n", "i", "r", "t", "s", " ", "a"], Array.prototype.toReversed.call('a string'));
assertSameContent(["a"], Array.prototype.toReversed.call("a"));
assertSameContent([], Array.prototype.toReversed.call(""));
