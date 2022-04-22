/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function outer() {
  var counter = 0;

  function inner() {
    class Group {};
    let Klass = class {
        constructor() {
            counter++;
        }
    };
  }

  inner();
}

outer();
