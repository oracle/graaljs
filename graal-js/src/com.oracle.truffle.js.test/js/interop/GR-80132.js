/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option crypto=true
 */

var ByteBuffer = Java.type('java.nio.ByteBuffer');
var backing = ByteBuffer.allocateDirect(16);
var javaView = new Uint8Array(backing).subarray(1, 5);
crypto.getRandomValues(javaView);
