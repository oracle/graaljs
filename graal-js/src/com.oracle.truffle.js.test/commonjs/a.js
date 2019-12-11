/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
console.log('a starting');
exports.done = false;
const b = require('./b.js');
console.log('in a, b.done = ' + b.done);
exports.done = true;
console.log('a done');
