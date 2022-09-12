/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify correct parsing of a for-in statement with an always-true delete expression as (an invalid) lhs expr.
 */

load("assert.js");

assertThrows(() => eval('for(x = delete 0 in "") {}'), SyntaxError); // Invalid left side value of for..in loop
assertThrows(() => eval('for(x = delete 0 of "") {}'), SyntaxError); // Invalid left side value of for..of loop
assertThrows(() => eval('(async function() { for await(x = delete 0 of "") {}})'), SyntaxError);

eval('for(var x = delete (Math.max([[1]], -14)) in new Array(4)) { this.v2 = x; }');
assertThrows(() => eval('for(let x = delete (Math.max([[1]], -14)) in new Array(4)) { this.v2 = x; }'));
eval('for(x = delete 0; !delete true; x = delete "") {}');

assertThrows(() => eval(`
    (async function() {
        for await(let b of async function(y) {
            "use strict";
            o0.a1[({
                valueOf: function() {
                    for(x = delete "" in /x/g ) for (let i = 0; i < 999; i++) {}
                    return 9;
                }
            })];
        }) for await(let a of /*MARR*/[null]) throw e;
        eval.message;
    })`
), SyntaxError);
