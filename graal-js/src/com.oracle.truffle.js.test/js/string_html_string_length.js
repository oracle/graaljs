/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that the legacy HTML string methods enforce the implementation string length limit.
 *
 * @option string-length-limit=20
 */

load("./assert.js");

assertSame("<b>1234567890123</b>", "1234567890123".bold());
assertThrows(() => "12345678901234".bold(), RangeError);
assertThrows(() => "".link('"x"'), RangeError);
