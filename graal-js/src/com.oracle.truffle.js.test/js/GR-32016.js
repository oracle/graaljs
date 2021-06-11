/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of Object/Array.prototype.toString on proxy objects.
 */

load('assert.js');

// Ensure that Array.prototype.toString invokes Object.prototype.toString
delete Array.prototype.join;

[Object.prototype.toString, Array.prototype.toString].forEach(function (testedFunction) {
    // Proxy does not inherit [[ParameterMap]], [[ErrorData]], [[BooleanData]],
    // [[NumberData]], [[StringData]], [[DateValue]] and [[RegExpMatcher]] internal slots
    var argumentsObject = (function() { return arguments; })();
    assertSame('[object Object]', testedFunction.call(new Proxy(argumentsObject, {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(new Error(), {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(Object(true), {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(Object(42), {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(Object('foo'), {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(new Date(), {})));
    assertSame('[object Object]', testedFunction.call(new Proxy(/a/, {})));

    // Proxy inherits [[Call]] internal slot
    assertSame('[object Function]', testedFunction.call(new Proxy(function() {}, {})));

    // There is an explicit handling of array Proxy targets in Object.prototype.toString
    assertSame('[object Array]', testedFunction.call(new Proxy([], {})));
    // revoked
    var revocable = Proxy.revocable([], {});
    revocable.revoke();
    assertThrows(function() {
        testedFunction.call(revocable.proxy);
    }, TypeError);
    // revoked too late (after builtinTag was determined)
    revocable = Proxy.revocable([], {
        get: function (target, key, receiver) {
            if (key === Symbol.toStringTag) {
                revocable.revoke();
                return undefined;
            } else {
                return Reflect.get(target, key, receiver);
            }
        },
    });
    assertSame('[object Array]', testedFunction.call(revocable.proxy));    
});
