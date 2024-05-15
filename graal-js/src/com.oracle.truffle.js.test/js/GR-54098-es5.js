/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests correct error handling of invalid `this` object in ES5 RegExp.prototype.test and exec.
 *
 * @option ecmascript-version=5
 */

load("assert.js");

assertThrows(function() { RegExp.prototype.test.call({ exec: function() { return null; } }); }, TypeError);
assertThrows(function() { RegExp.prototype.test.call({ exec: function() { return null; } }, "input"); }, TypeError);

assertThrows(function() { RegExp.prototype.exec.call({ exec: function() { return null; } }); }, TypeError);
assertThrows(function() { RegExp.prototype.exec.call({ exec: function() { return null; } }, "input"); }, TypeError);
