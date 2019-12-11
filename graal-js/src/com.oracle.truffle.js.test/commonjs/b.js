/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
console.log('b starting');
exports.done = false;
const a = require('./a.js');
console.log('in b, a.done = ' + a.done);
exports.done = true;
console.log('b done');
