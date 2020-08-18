/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

'use strict';

// This test just verifies that the following code does not result
// in an internal error (it used to throw AssertionError).
// The test-case was derived from a GitHub report:
// https://github.com/graalvm/graaljs/issues/331

(function() {
    var f = function() {};
    f.apply(undefined, new java.util.ArrayList());
    f.apply(undefined, arguments);
})();

true;
