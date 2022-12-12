/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Operator overloading proposal.
 *
 * @option operator-overloading=true
 * @option v8-realm-builtin=true
 */

load("assert.js");

{
    class MyOp extends Operators({}) {}
    let MyOther = Realm.eval(Realm.create(), `(${MyOp})`);
    assertThrows(() => new MyOp() + new MyOther(), TypeError); // No overload found for +
}

{
    class MyObj extends Operators({
        "+"(a, b) {
            return a.value + b.value;
        }
    }) {
        value;
    }
    let MyOther = Realm.eval(Realm.create(), `(${MyObj})`);
    assertThrows(() => new MyObj() + new MyOther(), TypeError); // No overload found for +
}

{
    const MyOther = Realm.eval(Realm.create(), `(${
        class MyOther extends Operators({}) {
            value = 43;
        }
    })`);
    class MyObj extends Operators({}, {
        right: MyOther,
        "+"(a, b) {
            return a.value + b.value;
        }
    }) {
        value = 42;
    }

    assertSame(85, new MyObj() + new MyOther());
}
