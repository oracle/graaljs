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

let log = [];

function logged(element) {
   if(element.kind == "method") {
       const f = element.method;
       element.method = function(...args) {
           log.push(`enter ${element.key}`);
           for(arg of args) {
               log.push(`arg: ${arg}`);
           }
           f.call(this,...args);
           log.push(`exit ${element.key}`);
       }
   }
   return element;
}

class C{
   @logged
   m(arg) {}
}

new C().m(1);

assertSame("enter m", log[0]);
assertSame("arg: 1", log[1]);
assertSame("exit m", log[2]);
true;