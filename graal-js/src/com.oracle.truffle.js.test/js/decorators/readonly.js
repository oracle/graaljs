/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test for checking the order of properties in AggregateError.
 *
 * @option ecmascript-version=2022
 */

load('../assert.js');

 function readonly(element) {
    element.writable = false;
    return element;
 }

 class C{
    @readonly x = 5;
 }

assertThrows(function() {
    "use strict";
     let c = new C();
     c.x = 10;
},TypeError, '"x" is not a writable property of');

true;