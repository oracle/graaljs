/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests decorator evaluation and application order, replacement classes and methods, and initializers.
 * Covers public and private elements, including instance and static auto-accessors,
 * decorator-context access, and private-brand checks.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

const events = [];
const evaluated = (name, decorator) => {
    events.push(`evaluate ${name}`);
    return decorator;
};

function methodDecorator(value, context) {
    events.push(`decorate ${context.name}`);
    context.addInitializer(function () {
        events.push(`initializer ${context.name}`);
    });
    return function () {
        return value.call(this) + '!';
    };
}

function fieldDecorator(value, context) {
    events.push(`decorate ${context.static ? 'static ' : ''}${context.name}`);
    context.addInitializer(function () {
        events.push(`initializer ${context.static ? 'static ' : ''}${context.name}`);
    });
}

function classDecorator(value, context) {
    events.push(`decorate ${context.kind}`);
    context.addInitializer(function () {
        events.push('initializer class');
    });
    return class extends value {
        static replacement = true;
    };
}

const computedName = () => {
    events.push('computed name');
    return 'method';
};

@evaluated('class left', classDecorator) @evaluated('class right', classDecorator)
class C {
    @evaluated('method left', methodDecorator) @evaluated('method right', methodDecorator)
    [computedName()]() {
        return 'method';
    }

    @evaluated('static field', fieldDecorator)
    static field = (events.push('static field value'), 42);

    @evaluated('instance field', fieldDecorator)
    field2 = (events.push('instance field value'), 43);
}

assertSameContent([
    'evaluate class left', 'evaluate class right',
    'evaluate method left', 'evaluate method right', 'computed name',
    'evaluate static field', 'evaluate instance field',
    'decorate method', 'decorate method',
    'decorate static field', 'decorate field2',
    'decorate class', 'decorate class',
    'initializer static field', 'static field value',
    'initializer class', 'initializer class'
], events);
assertTrue(C.replacement);
assertSame(42, C.field);
assertSame('method!!', new C().method());
assertSameContent([
    'initializer method', 'initializer method',
    'initializer field2', 'instance field value'
], events.slice(-4));

function accessorDecorator(value, context) {
    assertSame('accessor', context.kind);
    return {
        get() {
            return value.get.call(this) + 1;
        },
        set: value.set,
        init(value) {
            return value + 1;
        }
    };
}

class AccessorDecorated {
    @accessorDecorator
    accessor value = 1;
}
assertSame(3, new AccessorDecorated().value);

function getterDecorator(value, context) {
    assertSame('getter', context.kind);
    return function () {
        return value.call(this) + 1;
    };
}

class GetterDecorated {
    @getterDecorator
    get value() {
        return 1;
    }
}
assertSame(2, new GetterDecorated().value);

const privateContexts = {};

function privateFieldDecorator(value, context) {
    assertSame(undefined, value);
    assertSame('field', context.kind);
    assertTrue(context.private);
    assertFalse(context.static);
    assertSame('#field', context.name);
    privateContexts.field = context;
    context.addInitializer(function () {
        this.fieldInitializer = true;
    });
}

function privateMethodDecorator(value, context) {
    assertSame('method', context.kind);
    assertTrue(context.private);
    assertFalse(context.static);
    assertSame('#method', context.name);
    privateContexts.method = context;
    return function () {
        return value.call(this) + 1;
    };
}

function privateAutoAccessorDecorator(value, context) {
    assertSame('accessor', context.kind);
    assertTrue(context.private);
    assertFalse(context.static);
    assertSame('#auto', context.name);
    privateContexts.auto = context;
    return {
        get() {
            return value.get.call(this) + 1;
        },
        set(newValue) {
            return value.set.call(this, newValue * 2);
        },
        init(initialValue) {
            return initialValue + 1;
        }
    };
}

function privateStaticFieldDecorator(value, context) {
    assertSame(undefined, value);
    assertSame('field', context.kind);
    assertTrue(context.private);
    assertTrue(context.static);
    assertSame('#staticField', context.name);
    privateContexts.staticField = context;
    context.addInitializer(function () {
        this.staticFieldInitializer = true;
    });
}

class PrivateDecorated {
    @privateFieldDecorator
    #field = 1;

    @privateMethodDecorator
    #method() {
        return 10;
    }

    @privateAutoAccessorDecorator
    accessor #auto = 2;

    @privateStaticFieldDecorator
    static #staticField = 3;

    get field() {
        return this.#field;
    }

    get method() {
        return this.#method();
    }

    get auto() {
        return this.#auto;
    }

    set auto(value) {
        this.#auto = value;
    }

    static get staticField() {
        return this.#staticField;
    }
}

const privateInstance = new PrivateDecorated();
assertTrue(privateInstance.fieldInitializer);
assertSame(1, privateInstance.field);
assertSame(1, privateContexts.field.access.get.call(privateInstance));
privateContexts.field.access.set.call(privateInstance, 5);
assertSame(5, privateInstance.field);
assertSame(11, privateInstance.method);
assertSame(11, privateContexts.method.access.get.call(privateInstance).call(privateInstance));
assertSame(4, privateInstance.auto);
privateInstance.auto = 5;
assertSame(11, privateInstance.auto);
assertSame(11, privateContexts.auto.access.get.call(privateInstance));
privateContexts.auto.access.set.call(privateInstance, 7);
assertSame(15, privateInstance.auto);
assertTrue(PrivateDecorated.staticFieldInitializer);
assertSame(3, privateContexts.staticField.access.get.call(PrivateDecorated));
privateContexts.staticField.access.set.call(PrivateDecorated, 4);
assertSame(4, PrivateDecorated.staticField);

let privateAccessError;
try {
    privateContexts.field.access.get.call({});
} catch (error) {
    privateAccessError = error;
}
assertTrue(privateAccessError.message.includes('#field'));
assertFalse(privateAccessError.message.includes('BackingStorageKey'));

let privateMethodAccessError;
try {
    privateContexts.method.access.get.call({});
} catch (error) {
    privateMethodAccessError = error;
}
assertSame('Receiver must be an instance of class', privateMethodAccessError.message);

let privateAutoAccessorError;
try {
    privateContexts.auto.access.get.call({});
} catch (error) {
    privateAutoAccessorError = error;
}
assertSame('Receiver must be an instance of class', privateAutoAccessorError.message);

let staticAutoContext;
function staticAutoDecorator(value, context) {
    assertTrue(context.private);
    assertTrue(context.static);
    staticAutoContext = context;
    return {
        get() { return value.get.call(this) + 1; },
        set(newValue) { value.set.call(this, newValue * 2); },
        init(initialValue) { return initialValue + 1; }
    };
}
class StaticAutoDecorated {
    @staticAutoDecorator
    static accessor #auto = 2;
    static get auto() { return this.#auto; }
    static set auto(value) { this.#auto = value; }
}
assertSame(4, StaticAutoDecorated.auto);
StaticAutoDecorated.auto = 5;
assertSame(11, StaticAutoDecorated.auto);
assertSame(11, staticAutoContext.access.get.call(StaticAutoDecorated));
staticAutoContext.access.set.call(StaticAutoDecorated, 7);
assertSame(15, StaticAutoDecorated.auto);
assertThrows(() => staticAutoContext.access.get.call({}), TypeError);
