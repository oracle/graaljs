/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

{
    function extendClass(value, { kind, name }) {
        assertSame('class', kind)
        assertSame('Decorated', name);
        return class Extended extends value {
            constructor(...args) {
                super(...args);
                assertSame(1, args.length);
                assertSame(42, args[0]);
            }

            static original = value;
        }
    }

    @extendClass
    class Decorated {
        static #sf = 'hei';

        static testPrivateStaticField(cls = Decorated) {
            return cls.#sf;
        }

        static #sm() {
            return 'hej';
        }

        static testPrivateStaticMethod(cls = Decorated) {
            return cls.#sm();
        }

        #im() {
            return 'hey';
        }

        testPrivateInstanceMethod(obj = this) {
            return obj.#im();
        }

        #if = 'hai';

        testPrivateInstanceField(obj = this) {
            return obj.#if;
        }
    }

    assertSame('Extended', Decorated.name);

    let Original = Decorated.original;
    assertTrue(typeof Original === 'function' && Original !== Decorated);

    let decorated = new Decorated(42);
    let original = new Original(42);
    assertTrue(decorated instanceof Original);
    assertFalse(original instanceof Decorated);

    // static fields are defined on the new class constructor whereas static methods are defined on the original class constructor
    assertSame('hei', Decorated.testPrivateStaticField(Decorated));
    assertSame('hei', Original.testPrivateStaticField(Decorated));
    assertSame('hei', Decorated.testPrivateStaticField());
    assertSame('hei', Original.testPrivateStaticField());
    assertThrows(() => Decorated.testPrivateStaticField(Original), TypeError);
    assertThrows(() => Original.testPrivateStaticField(Original), TypeError);

    // static private methods are added to the original class constructor (?)
    assertSame('hej', Decorated.testPrivateStaticMethod(Original));
    assertSame('hej', Original.testPrivateStaticMethod(Original));
    assertThrows(() => Decorated.testPrivateStaticMethod(Decorated), TypeError);
    assertThrows(() => Original.testPrivateStaticMethod(Decorated), TypeError);
    assertThrows(() => Decorated.testPrivateStaticMethod(), TypeError);
    assertThrows(() => Original.testPrivateStaticMethod(), TypeError);

    // private instance methods are added to the original class, but the new class inherits from it, so the brand check should not fail
    assertSame('hey', decorated.testPrivateInstanceMethod(decorated));
    assertSame('hey', original.testPrivateInstanceMethod(decorated));
    assertSame('hey', decorated.testPrivateInstanceMethod());
    assertSame('hey', original.testPrivateInstanceMethod());
    assertSame('hey', decorated.testPrivateInstanceMethod(original));
    assertSame('hey', original.testPrivateInstanceMethod(original));

    assertSame('hai', decorated.testPrivateInstanceField(decorated));
    assertSame('hai', original.testPrivateInstanceField(decorated));
    assertSame('hai', decorated.testPrivateInstanceField());
    assertSame('hai', original.testPrivateInstanceField());
    assertSame('hai', decorated.testPrivateInstanceField(original));
    assertSame('hai', original.testPrivateInstanceField(original));
}

{
    function replaceClass(value, {kind, name}) {
        assertSame('class', kind);
        assertSame('Decorated', name);
        return class Replaced {
            constructor(...args) {
                assertSame(1, args.length);
                assertSame(42, args[0]);
            }

            static original = value;
        }
    }

    @replaceClass
    class Decorated {
        static #sf = 'hei';

        static testPrivateStaticField(cls = Decorated) {
            return cls.#sf;
        }

        static #sm() {
            return 'hej';
        }

        static testPrivateStaticMethod(cls = Decorated) {
            return cls.#sm();
        }

        #im() {
            return 'hey';
        }

        testPrivateInstanceMethod(obj = this) {
            return obj.#im();
        }

        #if = 'hai';

        testPrivateInstanceField(obj = this) {
            return obj.#if;
        }
    }

    assertSame('Replaced', Decorated.name);

    let Original = Decorated.original;
    assertTrue(typeof Original === 'function' && Original !== Decorated);

    let decorated = new Decorated(42);
    let original = new Original(42);
    assertFalse(decorated instanceof Original);
    assertFalse(original instanceof Decorated);

    assertFalse('testPrivateStaticField' in Decorated);
    assertSame('hei', Original.testPrivateStaticField(Decorated));
    assertSame('hei', Original.testPrivateStaticField());
    assertThrows(() => Original.testPrivateStaticField(Original), TypeError);

    assertFalse('testPrivateStaticMethod' in Decorated);
    assertSame('hej', Original.testPrivateStaticMethod(Original));
    assertThrows(() => Original.testPrivateStaticMethod(Decorated), TypeError);
    assertThrows(() => Original.testPrivateStaticMethod(), TypeError);

    assertFalse('testPrivateInstanceMethod' in decorated);
    assertThrows(() => original.testPrivateInstanceMethod(decorated), TypeError);
    assertSame('hey', original.testPrivateInstanceMethod());
    assertSame('hey', original.testPrivateInstanceMethod(original));

    assertFalse('testPrivateInstanceField' in decorated);
    assertThrows(() => original.testPrivateInstanceField(decorated), TypeError);
    assertSame('hai', original.testPrivateInstanceField());
    assertSame('hai', original.testPrivateInstanceField(original));
}
