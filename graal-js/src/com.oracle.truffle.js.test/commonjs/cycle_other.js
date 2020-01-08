/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
console.log('other starting at ' + __filename);
exports.done = false;
const main = require('./cycle_main.js');
console.log('main.done = ' + main.done);
exports.done = true;
console.log('other done');
exports.num = 42;