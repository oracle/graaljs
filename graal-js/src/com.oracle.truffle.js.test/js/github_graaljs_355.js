/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests that Array.prototype.reverse() does not touch the middle element
// as reported at https://github.com/graalvm/graaljs/issues/355

load('assert.js');

// should not throw
Object.freeze([]).reverse();
Object.freeze([42]).reverse();
Array.prototype.reverse.call(
    new Proxy([211], {
        get: function(target, prop) {
            if (prop === 'length') {
                return target.length;
            } else {
                fail('Unexpected get of ' + prop);
            }
        },
        set: function(target, prop) {
            fail('Unexpected set of ' + prop);
        },
        has: function(taget, prop) {
            fail('Unexpected has of ' + prop);
        }
    })
);

assertThrows(function() {
    Object.freeze(['foo', 'bar']).reverse();
}, TypeError);

true;
