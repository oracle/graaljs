/**
 * @option js.extractors
 */

/**
 * Runtime Semantics - InvokeCustomMatcherOrThrow
 *
 * Tests the runtime behavior of custom matcher invocation:
 * - Type check: extractor must be an Object
 * - Method resolution: Symbol.customMatcher must exist
 * - Invocation: matcher is called with the value
 * - Result type check: matcher result must be an Object
 * - Iterator retrieval: result must be iterable
 */

load('../assert.js');
load('common.js');

// Type Check: Extractor is Not an Object
{
    const nonObjectValues = [
        ['undefined', undefined],
        ['null', null],
        ['number', 42],
        ['string', "string"],
        ['boolean', true]
    ];

    for (const [type, value] of nonObjectValues) {
        let caught = false;
        try {
            const Extractor = value;
            const Extractor(x) = { x: 1 };
            fail(`Should have thrown TypeError for ${type} extractor`);
        } catch (e) {
            caught = true;
            assertTrue(e instanceof TypeError, `Expected TypeError for ${type} extractor`);
        }
        assertTrue(caught, `Exception should have been thrown for ${type}`);
    }
}

// Method Resolution: [Symbol.customMatcher] is undefined, null, or not a function
{
    const testCases = [
        ['undefined', class NoMatcher {}],
        ['null', class NullMatcher { static [Symbol.customMatcher] = null; }],
        ['not a function', class NotAFunction { static [Symbol.customMatcher] = 42; }]
    ];

    for (const [description, MatcherClass] of testCases) {
        let caught = false;
        try {
            const MatcherClass(x) = new MatcherClass();
            fail(`Should have thrown TypeError when customMatcher is ${description}`);
        } catch (e) {
            caught = true;
            assertTrue(e instanceof TypeError, `Expected TypeError when customMatcher is ${description}`);
        }
        assertTrue(caught, `Exception should have been thrown for ${description}`);
    }
}

// Invocation: Matcher is Called with Correct Value and Can Throw Errors
{
    // Test that matcher receives the correct value
    let receivedValue = null;
    class ChecksArgument {
        static [Symbol.customMatcher](value) {
            receivedValue = value;
            return [1, 2];
        }
    }
    const testValue = { x: 10, y: 20 };
    const ChecksArgument(a, b) = testValue;
    assertSame(testValue, receivedValue, 'Matcher should receive the RHS value');
    assertSame(1, a);
    assertSame(2, b);

    // Test that matcher errors are propagated
    const errorCases = [
        ['Error', class { static [Symbol.customMatcher]() { throw new Error("matcher error"); } }, "matcher error"],
        ['TypeError', class { static [Symbol.customMatcher]() { throw new TypeError("custom type error"); } }, "custom type error"]
    ];

    for (const [type, MatcherClass, expectedMsg] of errorCases) {
        let caught = false;
        try {
            const MatcherClass(x) = new MatcherClass();
            fail(`Should have propagated ${type}`);
        } catch (e) {
            caught = true;
            assertTrue(e instanceof Error);
            assertSame(expectedMsg, e.message);
        }
        assertTrue(caught, `${type} should have been thrown`);
    }
}

// Invocation: Matcher Return Value is Used
{
    const Extractor = class {
        static [Symbol.customMatcher](subject) {
            // Return a custom array, not derived from subject
            return [1, 2, 3];
        }
    };

    const Extractor(a, b, c) = "any value";

    assertSame(1, a);
    assertSame(2, b);
    assertSame(3, c);
}

// Result Type Check: Matcher Result Must Be an Object
{
    const invalidResults = [
        ['undefined', undefined],
        ['null', null],
        ['number', 42],
        ['string', "string"],
        ['boolean', true]
    ];

    for (const [type, result] of invalidResults) {
        class ReturnsBad {
            static [Symbol.customMatcher]() {
                return result;
            }
        }

        let caught = false;
        try {
            const ReturnsBad(x) = new ReturnsBad();
            fail(`Should have thrown TypeError when matcher returns ${type}`);
        } catch (e) {
            caught = true;
            assertTrue(e instanceof TypeError, `Expected TypeError when matcher returns ${type}`);
        }
        assertTrue(caught, `Exception should have been thrown for ${type}`);
    }
}

// Iterator Retrieval: Result is Iterable Array
{
    const Extractor = class {
        static [Symbol.customMatcher](subject) {
            return [1, 2, 3];
        }
    };

    const Extractor(a, b, c) = "value";

    assertSame(1, a);
    assertSame(2, b);
    assertSame(3, c);
}

// Success Case: Proper Invocation with Custom Iterable
{
    class CustomIterableMatcher {
        constructor(val) { this.val = val; }
        static [Symbol.customMatcher](obj) {
            return {
                [Symbol.iterator]() {
                    let i = 0;
                    const values = [obj.val, obj.val * 2, obj.val * 3];
                    return {
                        next() {
                            if (i < values.length) {
                                return { value: values[i++], done: false };
                            }
                            return { done: true };
                        }
                    };
                }
            };
        }
    }

    const CustomIterableMatcher(x, y, z) = new CustomIterableMatcher(5);
    assertSame(5, x);
    assertSame(10, y);
    assertSame(15, z);
}

// Iterator Retrieval: Result is Custom Iterable (alternative implementation)
{
    const Extractor = class {
        static [Symbol.customMatcher](subject) {
            return {
                [Symbol.iterator]() {
                    let i = 0;
                    return {
                        next() {
                            if (i < 3) {
                                return { value: i++, done: false };
                            }
                            return { done: true };
                        }
                    };
                }
            };
        }
    };

    const Extractor(a, b, c) = "value";

    assertSame(0, a);
    assertSame(1, b);
    assertSame(2, c);
}

// Success Case: Matcher with Generator Function
{
    class GeneratorMatcher {
        constructor(...values) {
            this.values = values;
        }

        static [Symbol.customMatcher](obj) {
            return {
                *[Symbol.iterator]() {
                    yield* obj.values;
                }
            };
        }
    }

    const GeneratorMatcher(a, b, c) = new GeneratorMatcher(7, 8, 9);
    assertSame(7, a);
    assertSame(8, b);
    assertSame(9, c);
}

// Iterator Retrieval: Result is Generator (alternative implementation)
{
    const Extractor = class {
        static [Symbol.customMatcher](subject) {
            return (function*() {
                yield 10;
                yield 20;
                yield 30;
            })();
        }
    };

    const Extractor(a, b, c) = "value";

    assertSame(10, a);
    assertSame(20, b);
    assertSame(30, c);
}

// Iterator Retrieval: Matcher Result Must Be Iterable
{
    const invalidIterableCases = [
        ['no Symbol.iterator', {}],
        ['Symbol.iterator not a function', { [Symbol.iterator]: 42 }],
        ['Symbol.iterator returns null', { [Symbol.iterator]() { return null; } }]
    ];

    for (const [description, result] of invalidIterableCases) {
        class BadIterable {
            static [Symbol.customMatcher]() {
                return result;
            }
        }

        let caught = false;
        try {
            const BadIterable(x) = new BadIterable();
            fail(`Should have thrown TypeError: ${description}`);
        } catch (e) {
            caught = true;
            assertTrue(e instanceof TypeError, `Expected TypeError: ${description}`);
        }
        assertTrue(caught, `Exception should have been thrown: ${description}`);
    }
}

// Success Case: Proper Invocation and Binding with Array
{
    class ArrayMatcher {
        constructor(val) { this.val = val; }
        static [Symbol.customMatcher](obj) {
            return [obj.val, obj.val + 1];
        }
    }

    const ArrayMatcher(a, b) = new ArrayMatcher(10);
    assertSame(10, a);
    assertSame(11, b);
}

// Success Case: Matcher Accessing Object Properties
{
    class Point {
        constructor(x, y) {
            this.x = x;
            this.y = y;
        }

        static [Symbol.customMatcher](point) {
            return [point.x, point.y];
        }
    }

    const Point(px, py) = new Point(100, 200);
    assertSame(100, px);
    assertSame(200, py);
}

// Matcher Accesses Subject Properties (using common Pair)
{
    const Pair(x, y) = new Pair(100, 200);

    assertSame(100, x);
    assertSame(200, y);
}

// Matcher Uses Private Fields
{
    class Wrapper {
        #value;

        constructor(value) {
            this.#value = value;
        }

        static [Symbol.customMatcher](subject) {
            // Check for private field and return its value
            if (#value in subject) {
                return [subject.#value];
            }
            throw new Error("Invalid subject");
        }
    }

    const Wrapper(x) = new Wrapper("private");
    assertSame("private", x);
}

// Success Case: Matcher Returning Empty Iterable
{
    class EmptyMatcher {
        static [Symbol.customMatcher]() {
            return [];
        }
    }

    const EmptyMatcher() = new EmptyMatcher();
    // No bindings, should succeed
}

// Matcher Returns Empty Array (alternative test)
{
    const EmptyExtractor = class {
        static [Symbol.customMatcher](subject) {
            return [];
        }
    };

    // Empty pattern should work
    const EmptyExtractor() = "value";
    assertTrue(true); // Just verify no error
}

// Matcher Returns Array with Insufficient Elements
{
    const InsufficientExtractor = class {
        static [Symbol.customMatcher](subject) {
            return [1]; // Only one element
        }
    };

    // Destructuring with more variables than values
    const InsufficientExtractor(a, b, c) = "value";

    assertSame(1, a);
    assertSame(undefined, b); // Should be undefined
    assertSame(undefined, c); // Should be undefined
}

// Success Case: Matcher with More Values Than Bindings
{
    class ExtraMatcher {
        static [Symbol.customMatcher]() {
            return [1, 2, 3, 4, 5];
        }
    }

    const ExtraMatcher(a, b) = new ExtraMatcher();
    assertSame(1, a);
    assertSame(2, b);
    // Extra values are ignored
}

// Matcher Returns Array with Extra Elements (alternative test)
{
    const ExtraExtractor = class {
        static [Symbol.customMatcher](subject) {
            return [1, 2, 3, 4, 5];
        }
    };

    // Only destructure first two
    const ExtraExtractor(a, b) = "value";

    assertSame(1, a);
    assertSame(2, b);
    // Extra elements are ignored
}

// Matcher is Called Once Per Extraction
{
    let callCount = 0;

    const CountingExtractor = class {
        static [Symbol.customMatcher](subject) {
            callCount++;
            return [callCount];
        }
    };

    const CountingExtractor(x) = "value";
    assertSame(1, x);
    assertSame(1, callCount); // Called exactly once
}

// Different Subjects Call Matcher with Different Values
{
    const values = [];

    const RecordingExtractor = class {
        static [Symbol.customMatcher](subject) {
            values.push(subject);
            return [subject];
        }
    };

    const RecordingExtractor(a) = "first";
    const RecordingExtractor(b) = "second";

    assertSame("first", a);
    assertSame("second", b);
    assertSameContent(["first", "second"], values);
}

// Matcher Returns Object with Symbol.iterator
{
    const CustomIterableExtractor = class {
        static [Symbol.customMatcher](subject) {
            return {
                data: [5, 10, 15],
                [Symbol.iterator]() {
                    let index = 0;
                    const data = this.data;
                    return {
                        next() {
                            if (index < data.length) {
                                return { value: data[index++], done: false };
                            }
                            return { done: true };
                        }
                    };
                }
            };
        }
    };

    const CustomIterableExtractor(a, b, c) = "value";

    assertSame(5, a);
    assertSame(10, b);
    assertSame(15, c);
}

// Matcher with This Context
{
    const ExtractorWithThis = class {
        static data = "static-data";

        static [Symbol.customMatcher](subject) {
            // 'this' should refer to the class
            return [this.data];
        }
    };

    const ExtractorWithThis(x) = "value";
    assertSame("static-data", x);
}

