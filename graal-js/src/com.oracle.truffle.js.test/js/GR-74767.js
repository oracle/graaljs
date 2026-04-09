/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var protoDesc = Object.getOwnPropertyDescriptor(Object.prototype, "__proto__");
assertSame(0, protoDesc.get.length);
assertSame(1, protoDesc.set.length);
