/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js")

assertSame('foo', eval("fooStatement\n: 'foo'"));
assertSame('bar', eval("barStatement/**/: 'bar'"));

// Original test-case (used to result in SyntaxError)
function example() {
    return
    {
      key
      :
      'value'
    };
}
