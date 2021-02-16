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

function measure(element) {
    let m = element.method;
    element.method = function(...args) {
        let time = new Date().getTime();
        m.call(this, ...args);
        console.log(new Date().getTime() - time);
    }
    return element;
}

class C {
    @measure
    m(arg) {}
}

new C().m(1);

true;