/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

const apm = Array.prototype.map

const moduleIds = ['a', 'b', 'c'];

function foo() {

  class NativeModule {
    
    static fieldy = apm.call(moduleIds, id => id);    

  }

  new NativeModule();
};

for (var i = 0; i < 100000; i++) {
  foo();
}
