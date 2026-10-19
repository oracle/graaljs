/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var receiver = {
    eval: function(value) {
        assertSame(receiver, this);
        return value;
    }
};

with (receiver) {
    assertSame(undefined, eval());
    assertSame('argument', eval('argument'));
}

(function() {
    const events = [];
    const scope = {
        get eval() {
            events.push('get');
            return function(value, extra) {
                'use strict';
                assertSame(scope, this);
                events.push('call');
                return value + extra;
            };
        }
    };
    with (scope) {
        assertSame(42, eval((events.push('arg1'), 40), (events.push('arg2'), 2)));
    }
    assertSameContent(['get', 'arg1', 'arg2', 'call'], events);
})();

(function(eval) {
    const scope = {
        eval: function(value) {
            'use strict';
            return [this, value];
        },
        [Symbol.unscopables]: {eval: false}
    };
    with (scope) {
        // Changing lookup conditions during argument evaluation must not change the receiver.
        const result = eval((scope[Symbol.unscopables].eval = true, 42));
        assertSame(scope, result[0]);
        assertSame(42, result[1]);
        const fallback = eval(43);
        assertSame(undefined, fallback[0]);
        assertSame(43, fallback[1]);
        scope[Symbol.unscopables].eval = false;
        const indirect = (0, eval)(44);
        assertSame(undefined, indirect[0]);
        assertSame(44, indirect[1]);
    }
})(function(value) {
    'use strict';
    return [this, value];
});

(function() {
    const scope = new Proxy({eval: function(value) {
        'use strict';
        assertSame(scope, this);
        return value;
    }}, {});
    with (scope) {
        assertSame(42, eval(...[42]));
    }
})();

(function() {
    const local = 42;
    const lexicalThis = this;
    with ({eval: globalThis.eval}) {
        assertSame(42, eval('local'));
        assertSame(lexicalThis, eval('this'));
    }
}).call({});

(function() {
    const scope = {};
    function invoke(value) {
        const local = 42;
        with (scope) {
            return eval(value);
        }
    }
    function replacement(value) {
        'use strict';
        assertSame(scope, this);
        return value;
    }
    for (let i = 0; i < 3; i++) {
        scope.eval = globalThis.eval;
        assertSame(42, invoke('local'));
        scope.eval = replacement;
        assertSame('local', invoke('local'));
    }
})();
