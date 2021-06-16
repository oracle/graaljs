/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that java.lang.String methods can be invoked on lazy strings.
 * 
 * @option nashorn-compat
 * @option ecmascript-version=6
 */

load('assert.js');

var lazyString = Debug.createLazyString('x'.repeat(20), 'y'.repeat(20));

assertSame(40, lazyString.length());

var bytes = lazyString.getBytes();
assertSame(40, bytes.length);
for (var i = 0; i < bytes.length; i++) {
    assertSame(lazyString.codePointAt(i), bytes[i]);
}
