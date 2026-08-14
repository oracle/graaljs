/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of computed super-property reference evaluation.
 *
 * @option ecmascript-version=2021
 * @option unhandled-rejections=throw
 */

load('assert.js');

function testSuperComputedPropertyKeyEvaluationOrder() {
    var keyEvaluated = false;
    class Base {}
    class DerivedBeforeSuper extends Base {
        constructor() {
            super[(keyEvaluated = true, 'property')];
        }
    }
    assertThrows(function() {
        new DerivedBeforeSuper();
    }, ReferenceError);
    assertFalse(keyEvaluated);

    var first = {property: 'first'};
    var second = {property: 'second'};
    class ReadDerived {
        read() {
            return super[(Object.setPrototypeOf(ReadDerived.prototype, second), 'property')];
        }
    }
    Object.setPrototypeOf(ReadDerived.prototype, first);
    assertSame('second', new ReadDerived().read());

    var setter = '';
    first = {set property(value) {
        setter = 'first';
    }};
    second = {set property(value) {
        setter = 'second';
    }};
    class WriteDerived {
        write() {
            return super[(Object.setPrototypeOf(WriteDerived.prototype, second), 'property')] = 42;
        }
    }
    Object.setPrototypeOf(WriteDerived.prototype, first);
    assertSame(42, new WriteDerived().write());
    assertSame('second', setter);

    first = {property: 1};
    second = {property: 41};
    class CompoundDerived {
        add() {
            return super[(Object.setPrototypeOf(CompoundDerived.prototype, second), 'property')] += 1;
        }
    }
    var compoundDerived = new CompoundDerived();
    Object.setPrototypeOf(CompoundDerived.prototype, first);
    assertSame(42, compoundDerived.add());
    assertSame(42, compoundDerived.property);

    first = {property: true};
    second = {property: false};
    class AndDerived {
        assign() {
            return super[(Object.setPrototypeOf(AndDerived.prototype, second), 'property')] &&= 42;
        }
    }
    var andDerived = new AndDerived();
    Object.setPrototypeOf(AndDerived.prototype, first);
    assertFalse(andDerived.assign());
    assertFalse(Object.prototype.hasOwnProperty.call(andDerived, 'property'));

    first = {property: false};
    second = {property: true};
    var conversionCount = 0;
    var key = {
        [Symbol.toPrimitive]: function() {
            conversionCount++;
            Object.setPrototypeOf(OrDerived.prototype, second);
            return 'property';
        }
    };
    class OrDerived {
        assign() {
            return super[key] ||= 42;
        }
    }
    var orDerived = new OrDerived();
    Object.setPrototypeOf(OrDerived.prototype, first);
    assertSame(42, orDerived.assign());
    assertSame(1, conversionCount);
    assertTrue(Object.prototype.hasOwnProperty.call(orDerived, 'property'));

    first = {property: null};
    second = {property: 0};
    class NullishDerived {
        assign() {
            return super[(Object.setPrototypeOf(NullishDerived.prototype, second), 'property')] ??= 42;
        }
    }
    var nullishDerived = new NullishDerived();
    Object.setPrototypeOf(NullishDerived.prototype, first);
    assertSame(0, nullishDerived.assign());
    assertFalse(Object.prototype.hasOwnProperty.call(nullishDerived, 'property'));
}

testSuperComputedPropertyKeyEvaluationOrder();

function testDeleteSuperProperty() {
    var keyEvaluated = false;
    class Base {}
    class Derived extends Base {
        deleteProperty() {
            delete super[(keyEvaluated = true, 'property')];
        }
    }
    assertThrows(function() {
        new Derived().deleteProperty();
    }, ReferenceError);
    assertTrue(keyEvaluated);
}

testDeleteSuperProperty();

function testSuperPropertyAssignment() {
    var getterCalled = false;
    var setterCalled = false;
    var first = {
        get property() {
            assertSame(derived, this);
            getterCalled = true;
            return 1;
        },
        set property(value) {
            assertSame(derived, this);
            setterCalled = true;
            this.value = value;
        }
    };
    var second = {property: 41};
    class Derived {
        add() {
            return super.property += (Object.setPrototypeOf(Derived.prototype, second), 1);
        }
    }
    var derived = new Derived();
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame(2, derived.add());
    assertSame(2, derived.value);
    assertTrue(getterCalled);
    assertTrue(setterCalled);
}

testSuperPropertyAssignment();

function testSuperLogicalPropertyAssignment() {
    var setterCalled = false;
    var first = {
        get property() {
            assertSame(derived, this);
            return false;
        },
        set property(value) {
            assertSame(derived, this);
            assertSame(42, value);
            setterCalled = true;
        }
    };
    var second = {property: true};
    class Derived {
        assign() {
            return super.property ||= (Object.setPrototypeOf(Derived.prototype, second), 42);
        }
    }
    var derived = new Derived();
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame(42, derived.assign());
    assertTrue(setterCalled);
    assertFalse(Object.prototype.hasOwnProperty.call(derived, 'property'));
}

testSuperLogicalPropertyAssignment();

function testSuperComputedPropertyKeyEvaluationOrderWithYield() {
    var first = {property: 'first', method() { return 'first:' + this.marker; }};
    var second = {property: 'second', method() { return 'second:' + this.marker; }};
    class Derived {
        *read() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')];
        }
        *call() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')]();
        }
        *optionalCall() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')]?.();
        }
        *write() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')] = 42;
        }
        *add() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')] += 1;
        }
        *andAssign() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')] &&= 42;
        }
        *orAssign() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')] ||= 42;
        }
        *nullishAssign() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), yield 'key')] ??= 42;
        }
    }
    function resume(iterator, key) {
        assertSame('key', iterator.next().value);
        return iterator.next(key || 'property').value;
    }

    var derived = new Derived();
    derived.marker = 'receiver';
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame('second', resume(derived.read()));
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame('second:receiver', resume(derived.call(), 'method'));
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame('second:receiver', resume(derived.optionalCall(), 'method'));

    var setter = '';
    first = {set property(value) { setter = 'first'; }};
    second = {set property(value) { setter = 'second'; }};
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame(42, resume(derived.write()));
    assertSame('second', setter);

    first = {property: 1};
    second = {property: 41};
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame(42, resume(derived.add()));
    assertSame(42, derived.property);
    delete derived.property;

    first = {property: true};
    second = {property: false};
    Object.setPrototypeOf(Derived.prototype, first);
    assertFalse(resume(derived.andAssign()));
    assertFalse(Object.prototype.hasOwnProperty.call(derived, 'property'));

    first = {property: false};
    second = {property: true};
    Object.setPrototypeOf(Derived.prototype, first);
    assertTrue(resume(derived.orAssign()));
    assertFalse(Object.prototype.hasOwnProperty.call(derived, 'property'));

    first = {property: null};
    second = {property: 0};
    Object.setPrototypeOf(Derived.prototype, first);
    assertSame(0, resume(derived.nullishAssign()));
    assertFalse(Object.prototype.hasOwnProperty.call(derived, 'property'));

    first = {property: false};
    second = {property: true};
    var conversionCount = 0;
    var key = {
        [Symbol.toPrimitive]: function() {
            conversionCount++;
            Object.setPrototypeOf(ConversionDerived.prototype, second);
            return 'property';
        }
    };
    class ConversionDerived {
        *assign() {
            return super[yield 'key'] ||= 42;
        }
    }
    var conversionDerived = new ConversionDerived();
    Object.setPrototypeOf(ConversionDerived.prototype, first);
    assertSame(42, resume(conversionDerived.assign(), key));
    assertSame(1, conversionCount);
    assertTrue(Object.prototype.hasOwnProperty.call(conversionDerived, 'property'));
}

testSuperComputedPropertyKeyEvaluationOrderWithYield();

function testSuperComputedPropertyKeyEvaluationOrderWithAwait() {
    var first = {property: 'first', method() { return 'first:' + this.marker; }};
    var second = {property: 'second', method() { return 'second:' + this.marker; }};
    class Derived {
        async read() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), await Promise.resolve('property'))];
        }
        async call() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), await Promise.resolve('method'))]();
        }
        async optionalCall() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), await Promise.resolve('method'))]?.();
        }
        async write() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), await Promise.resolve('property'))] = 42;
        }
        async orAssign() {
            return super[(Object.setPrototypeOf(Derived.prototype, second), await Promise.resolve('property'))] ||= 42;
        }
    }

    var derived = new Derived();
    derived.marker = 'receiver';
    Object.setPrototypeOf(Derived.prototype, first);
    derived.read().then(function(value) {
        assertSame('second', value);
        Object.setPrototypeOf(Derived.prototype, first);
        return derived.call();
    }).then(function(value) {
        assertSame('second:receiver', value);
        Object.setPrototypeOf(Derived.prototype, first);
        return derived.optionalCall();
    }).then(function(value) {
        assertSame('second:receiver', value);
        var setter = '';
        first = {set property(value) { setter = 'first'; }};
        second = {set property(value) { setter = 'second'; }};
        Object.setPrototypeOf(Derived.prototype, first);
        return derived.write().then(function(value) {
            assertSame(42, value);
            assertSame('second', setter);
        });
    }).then(function() {
        first = {property: false};
        second = {property: true};
        Object.setPrototypeOf(Derived.prototype, first);
        return derived.orAssign();
    }).then(function(value) {
        assertTrue(value);
        assertFalse(Object.prototype.hasOwnProperty.call(derived, 'property'));
    });
}

testSuperComputedPropertyKeyEvaluationOrderWithAwait();
