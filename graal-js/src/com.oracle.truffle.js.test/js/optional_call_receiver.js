/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Optional calls preserve the receiver of the underlying reference.
 */

load('assert.js');

function strictThis() {
    'use strict';
    return this;
}

var varThis = strictThis;
let letThis = strictThis;
const constThis = strictThis;

assertSame(undefined, strictThis());
assertSame(undefined, strictThis?.());
assertSame(undefined, strictThis?.(1));
assertSame(undefined, strictThis?.(1, 2));
assertSame(undefined, strictThis?.(...[1, 2]));
assertSame(undefined, (strictThis)?.());
assertSame(undefined, varThis?.());
assertSame(undefined, letThis?.());
assertSame(undefined, constThis?.());

// A sloppy callee must not expose the storage object for global lexical bindings.
function sloppyThis() {
    return this;
}
let sloppy = sloppyThis;
const secret = 1;
var scope = sloppy?.();
scope.secret = 2;
assertSame(1, secret);
assertSame(globalThis, scope);

(function () {
    const localThis = strictThis;
    assertSame(undefined, localThis?.());
})();

(function () {
    eval('var evalThis = strictThis;');
    assertSame(undefined, evalThis());
    assertSame(undefined, evalThis?.());
    assertSame(undefined, evalThis?.(1));
    assertSame(undefined, evalThis?.(1, 2));
    assertSame(undefined, evalThis?.(...[1, 2]));
    // An eval lookup may also fall back to a binding outside the eval scope.
    assertSame(undefined, strictThis?.());
})();

const object = {strictThis};
assertSame(object, object.strictThis?.());
assertSame(object, object['strictThis']?.());
assertSame(object, object?.strictThis());
assertSame(object, object?.strictThis?.());
assertSame(object, (object?.strictThis)?.());
assertSame(object, ((object?.strictThis))?.(...[]));
with (object) {
    assertSame(object, strictThis?.());
}
with ({}) {
    assertSame(undefined, strictThis?.());
}

class Base {
    method() {
        'use strict';
        return this;
    }
}
class Derived extends Base {
    #method = strictThis;
    check() {
        assertSame(this, super.method?.());
        assertSame(this, super['method']?.());
        assertSame(this, this.#method?.());
    }
}
new Derived().check();

// Resolve the reference once, before argument evaluation changes the lookup conditions.
(function () {
    const events = [];
    const scope = {
        get method() {
            events.push('get');
            return strictThis;
        },
        [Symbol.unscopables]: {method: false}
    };
    with (scope) {
        assertSame(scope, method?.((events.push('argument'), scope[Symbol.unscopables].method = true)));
    }
    assertSameContent(['get', 'argument'], events);
})();

let argumentEvaluated = false;
var nullFunction = null;
let undefinedFunction;
assertSame(undefined, nullFunction?.(argumentEvaluated = true));
assertSame(undefined, undefinedFunction?.(argumentEvaluated = true));
assertSame(false, argumentEvaluated);

var notCallable = {};
assertThrows(() => notCallable?.(), TypeError);
