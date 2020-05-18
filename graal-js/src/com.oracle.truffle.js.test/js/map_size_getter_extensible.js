/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of Map size accessor property extensibility.
 */

load('assert.js');

var descriptor = Object.getOwnPropertyDescriptor(Map.prototype, 'size');

assertTrue(Object.isExtensible(descriptor.get));

true;
