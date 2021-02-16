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

function stringLength(length) {
    return function (element) {
        let validateKey = "validate_" + element.key;
        let f = element.initialize;
        element.initialize = function() {
            let val = f.call(this);
            if(val.length < length) {
                this[validateKey] = true;
                return val;
            } else {
                this[validateKey] = false;
                return "";
            }
        };
        return element;
    };
}

class C {
    @stringLength(10)
    foo = "hello, world!";

    @stringLength(10)
    bar = "test";
}

assertSame(false, new C().validate_foo);
assertSame(true, new C().validate_bar);

true;