/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Legacy behavior: ArraySetLength needs to call valueOf() twice.
 */

var a = [1,2,3,4];
var valueOfCalled = 0;
var newLength = {
  valueOf: function() {
    valueOfCalled++;
    return 3;
  }
};
a.length = newLength;

if (2 !== valueOfCalled) throw valueOfCalled;
if (3 !== a.length) throw a.length;

true;
