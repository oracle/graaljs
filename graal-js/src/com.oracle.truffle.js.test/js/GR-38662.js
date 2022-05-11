/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that various operations on JSAdapter do not throw.
 * 
 * @option nashorn-compat
 */

load("assert.js");

// should not throw
Object.seal(new JSAdapter({}));
Object.freeze(new JSAdapter({}));
Object.preventExtensions(new JSAdapter({}));

assertSame(true, Object.isExtensible(new JSAdapter({})));
assertSame(undefined, Object.getOwnPropertyDescriptor(new JSAdapter({}), 'key'));
