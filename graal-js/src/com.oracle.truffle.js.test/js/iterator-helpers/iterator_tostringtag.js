/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests (Async)Iterator's @@toStringTag and built-in toString tag.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

let iterator = new (class extends Iterator {});
assertSame("Iterator", Iterator.prototype[Symbol.toStringTag]);
assertSame("[object Iterator]", Object.prototype.toString.call(iterator));
delete Iterator.prototype[Symbol.toStringTag];
assertSame("[object Object]", Object.prototype.toString.call(iterator));

let asyncIterator = new (class extends AsyncIterator {});
assertSame("Async Iterator", AsyncIterator.prototype[Symbol.toStringTag]);
assertSame("[object Async Iterator]", Object.prototype.toString.call(asyncIterator));
delete AsyncIterator.prototype[Symbol.toStringTag];
assertSame("[object Object]", Object.prototype.toString.call(asyncIterator));
