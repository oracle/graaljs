/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for V8-compatible no-side-effects receiver formatting in error messages.
 *
 * @option v8-compat
 */

load('assert.js');

function getterOnlyMessage(object) {
    'use strict';
    Object.defineProperty(object, 'status', {
        configurable: true,
        get() {
            return 1;
        }
    });
    try {
        object.status = 2;
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testOrdinaryObjectMessage() {
    'use strict';
    assertSame('Cannot set property status of #<Object> which has only a getter', getterOnlyMessage({}));
})();

(function testClassInstanceMessage() {
    'use strict';
    class Pool {
    }
    assertSame('Cannot set property status of #<Pool> which has only a getter', getterOnlyMessage(new Pool()));
})();

(function testProxyReceiverUsesTargetFormattingWithoutTraps() {
    'use strict';
    class Pool {
    }
    var sideEffects = [];
    var proxy = new Proxy(new Pool(), {
        get(target, key, receiver) {
            sideEffects.push(String(key));
            return Reflect.get(target, key, receiver);
        }
    });
    assertSame('Cannot set property status of #<Pool> which has only a getter', getterOnlyMessage(proxy));
    assertSameContent([], sideEffects);
})();

function incompatibleReceiverMessage(value) {
    try {
        Error.prototype.toString.call(value);
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testIncompatibleReceiverUndefined() {
    'use strict';
    assertSame('Method Error.prototype.toString called on incompatible receiver undefined', incompatibleReceiverMessage(undefined));
})();

(function testIncompatibleReceiverNull() {
    'use strict';
    assertSame('Method Error.prototype.toString called on incompatible receiver null', incompatibleReceiverMessage(null));
})();

(function testOriginalFunctionNameIsUsed() {
    'use strict';
    function Orig() {
    }
    Object.defineProperty(Orig, 'name', {value: 'Mutated'});
    assertSame('Cannot set property status of #<Orig> which has only a getter', getterOnlyMessage({constructor: Orig}));
})();

(function testToStringOverrideDisablesConstructorFormatting() {
    'use strict';
    assertSame('Cannot set property status of [object Object] which has only a getter', getterOnlyMessage({toString() { return 'x'; }}));
})();

(function testConstructorAccessorIsIgnoredWithoutSideEffects() {
    'use strict';
    var sideEffects = [];
    var proto = {};
    Object.defineProperty(proto, 'constructor', {
        get() {
            sideEffects.push('getter');
            return function Ignored() {
            };
        }
    });
    var object = Object.create(proto);
    assertSame('Cannot set property status of [object Object] which has only a getter', getterOnlyMessage(object));
    assertSameContent([], sideEffects);
})();

(function testCallableProxyConstructorIsIgnoredWithoutSideEffects() {
    'use strict';
    var sideEffects = [];
    function Orig() {
    }
    var proxy = new Proxy(Orig, {
        apply() {
            sideEffects.push('apply');
            return 0;
        },
        get(target, key, receiver) {
            sideEffects.push(String(key));
            return Reflect.get(target, key, receiver);
        }
    });
    assertSame('Cannot set property status of [object Object] which has only a getter', getterOnlyMessage({constructor: proxy}));
    assertSameContent([], sideEffects);
})();

(function testFallbackUsesDefaultToString() {
    'use strict';
    var date = new Date();
    date.constructor = 42;
    assertSame('Cannot set property status of [object Date] which has only a getter', getterOnlyMessage(date));
})();

function readOnlyMessage(object, key) {
    'use strict';
    key = key === undefined ? 'ro' : key;
    Object.defineProperty(object, key, {
        configurable: true,
        value: 1,
        writable: false
    });
    try {
        object[key] = 2;
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testReadOnlyPropertyOrdinaryObject() {
    'use strict';
    assertSame("Cannot assign to read only property 'ro' of object '#<Object>'", readOnlyMessage({}));
})();

(function testReadOnlyPropertyClassInstance() {
    'use strict';
    class Pool {
    }
    assertSame("Cannot assign to read only property 'ro' of object '#<Pool>'", readOnlyMessage(new Pool()));
})();

(function testReadOnlyPropertyArray() {
    'use strict';
    assertSame("Cannot assign to read only property 'ro' of object '[object Array]'", readOnlyMessage([]));
})();

(function testReadOnlyPropertyToStringOverrideFallback() {
    'use strict';
    assertSame("Cannot assign to read only property 'ro' of object '[object Object]'", readOnlyMessage({toString() { return 'x'; }}));
})();

function deleteMessage(object, key) {
    'use strict';
    key = key === undefined ? 'p' : key;
    Object.defineProperty(object, key, {
        configurable: false,
        value: 1
    });
    try {
        delete object[key];
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testDeletePropertyOrdinaryObject() {
    'use strict';
    assertSame("Cannot delete property 'p' of #<Object>", deleteMessage({}));
})();

(function testDeletePropertyArray() {
    'use strict';
    var array = [1];
    assertSame("Cannot delete property '0' of [object Array]", deleteMessage(array, '0'));
})();

function invalidPropertyDescriptorMessage(descriptor) {
    try {
        Object.defineProperty({}, 'x', descriptor);
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testInvalidPropertyDescriptorOrdinaryObject() {
    'use strict';
    assertSame('Invalid property descriptor. Cannot both specify accessors and a value or writable attribute, #<Object>',
        invalidPropertyDescriptorMessage({get() { return 1; }, value: 1}));
})();

function propertyNotFunctionMessage(callback) {
    try {
        callback();
        fail('missing TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError);
        return e.message;
    }
}

(function testPropertyNotFunctionMap() {
    'use strict';
    var original = Map.prototype.set;
    try {
        Map.prototype.set = 0;
        assertSame("'0' returned for property 'set' of object '#<Map>' is not a function",
            propertyNotFunctionMessage(function() {
                new Map([[1, 2]]);
            }));
    } finally {
        Map.prototype.set = original;
    }
})();

(function testPropertyNotFunctionWeakSet() {
    'use strict';
    var original = WeakSet.prototype.add;
    try {
        WeakSet.prototype.add = 0;
        assertSame("'0' returned for property 'add' of object '#<WeakSet>' is not a function",
            propertyNotFunctionMessage(function() {
                new WeakSet([{}]);
            }));
    } finally {
        WeakSet.prototype.add = original;
    }
})();
