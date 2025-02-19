/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Set.prototype.forEach should work with foreign/host callback.
 */

const v1 = Java.type("java.util.List").of;
const v3 = new Set();
const v4 = new Set(["a", "b"]);
v3.forEach(v1);
v4.forEach(v1);
