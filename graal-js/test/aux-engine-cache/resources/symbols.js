/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

(function main() {

    const kBindStreamsLazy = Symbol('kBindStreamsLazy');
    const kSymFor = Symbol.for('symFor');
    const aSymbol = Symbol('kSymbol');

    if (Object.is(Symbol('xx'), Symbol('xx'))) {
        console.log('wrong!');
    }

    function rep() {
        var foo = Object.create({
            [Symbol.toPrimitive] : (n) => n
        });

        foo[Symbol.toPrimitive] = (x) => {x*x};
        foo[Symbol.toPrimitive](42);


        var foo = Object.create({
            [kBindStreamsLazy] : (x) => x
        });

        foo[kBindStreamsLazy] = (x) => {x*x};
        foo[kBindStreamsLazy](42);


        var foo = Object.create({
            [kSymFor] : (x) => x
        });

        foo[kSymFor] = (x) => {x*x};
        foo[kSymFor](42);


        var foo = {
            [aSymbol] : x => x,
            bar : n => n
        };

        foo.bar(42);

    }

    for (var i = 0; i < 100000; i++) {
        rep();
    }

})();
