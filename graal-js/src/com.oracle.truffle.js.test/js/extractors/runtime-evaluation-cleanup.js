/**
 * @option js.extractors
 */

/**
 * Runtime Semantics - Evaluation & Cleanup
 *
 * Tests the runtime behavior of destructuring and iterator cleanup:
 * - Destructuring: IteratorBindingInitialization (standard array destructuring)
 * - Iterator Closing: Ensures iterator.return() is called after binding
 * - Error Handling: Iterator closing on exceptions during binding
 *
 * todo-extractors: There are some commented-out assertions related to iterator return() calls.
 *  These should be revisited once extractor behavior is finalized.
 */

load('../assert.js');

// Basic Destructuring: All values consumed
{
    let returnCalled = false;

    class BasicMatcher {
        constructor(...values) {
            this.values = values;
        }

        static [Symbol.customMatcher](obj) {
            let i = 0;
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < obj.values.length) {
                        return { value: obj.values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const obj = new BasicMatcher(1, 2, 3);
    const BasicMatcher(a, b, c) = obj;

    assertSame(1, a);
    assertSame(2, b);
    assertSame(3, c);
    assertTrue(returnCalled, 'Iterator return() should be called');
}

// Partial Destructuring: Not all values consumed
{
    let returnCalled = false;

    class PartialMatcher {
        constructor(...values) {
            this.values = values;
        }

        static [Symbol.customMatcher](obj) {
            let i = 0;
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < obj.values.length) {
                        return { value: obj.values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const obj = new PartialMatcher(1, 2, 3, 4, 5);
    const PartialMatcher(a, b) = obj;

    assertSame(1, a);
    assertSame(2, b);
    assertTrue(returnCalled, 'Iterator return() should be called even with extra values');
}

// Empty Destructuring: No bindings
{
    let returnCalled = false;

    class EmptyMatcher {
        static [Symbol.customMatcher]() {
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const EmptyMatcher() = new EmptyMatcher();
    assertTrue(returnCalled, 'Iterator return() should be called even with no bindings');
}

// Iterator Closing on Error: next() throws
{
    let returnCalled = false;

    class ThrowsInNext {
        static [Symbol.customMatcher]() {
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    throw new Error("fail during iteration");
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    let caught = false;
    try {
        const ThrowsInNext(x) = new ThrowsInNext();
        fail('Should have thrown error from next()');
    } catch (e) {
        caught = true;
        // assertTrue(returnCalled, 'Iterator return() should be called on error');
    }
    assertTrue(caught, 'Exception should have been thrown');
}

// Iterator Closing on Error: Binding initialization throws
{
    let returnCalled = false;

    class BindingThrows {
        static [Symbol.customMatcher]() {
            let count = 0;
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (count === 0) {
                        count++;
                        return { value: { get x() { throw new Error("getter error"); } }, done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    let caught = false;
    try {
        const BindingThrows({ x }) = new BindingThrows();
        fail('Should have thrown error from getter');
    } catch (e) {
        caught = true;
        assertSame("getter error", e.message);
        assertTrue(returnCalled, 'Iterator return() should be called on binding error');
    }
    assertTrue(caught, 'Exception should have been thrown');
}

// Iterator without return() method
{
    class NoReturnMethod {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1, 2, 3];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                }
                // No return() method
            };
        }
    }

    // Should not throw even if return() is not defined
    const NoReturnMethod(a, b, c) = new NoReturnMethod();
    assertSame(1, a);
    assertSame(2, b);
    assertSame(3, c);
}

// Iterator return() throws error
{
    let returnCalled = false;

    class ReturnThrows {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1, 2];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    throw new Error("return() error");
                }
            };
        }
    }

    let caught = false;
    try {
        const ReturnThrows(a, b) = new ReturnThrows();
        fail('Should have thrown error from return()');
    } catch (e) {
        caught = true;
        assertSame("return() error", e.message);
        assertTrue(returnCalled, 'return() should have been called');
    }
    assertTrue(caught, 'Exception should have been thrown');
}

// Iterator return() returns non-object
{
    let returnCalled = false;

    class BadReturnValue {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1, 2];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return null; // Invalid return value
                }
            };
        }
    }

    let caught = false;
    try {
        const BadReturnValue(a, b) = new BadReturnValue();
        fail('Should have thrown TypeError');
    } catch (e) {
        caught = true;
        assertTrue(e instanceof TypeError, 'Expected TypeError for non-object return value');
        assertTrue(returnCalled, 'return() should have been called');
    }
    assertTrue(caught, 'Exception should have been thrown');
}

// Default Values in Destructuring
{
    let returnCalled = false;

    class WithDefaults {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const WithDefaults(a, b = 99, c = 100) = new WithDefaults();
    assertSame(1, a);
    assertSame(99, b);
    assertSame(100, c);
    // assertTrue(returnCalled, 'Iterator return() should be called');
}

// Nested Destructuring with Iterator Cleanup
{
    let outerReturnCalled = false;
    let innerReturnCalled = false;

    class Inner {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [10, 20];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    innerReturnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    class Outer {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [new Inner(), 30];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    outerReturnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const Outer(Inner(x, y), z) = new Outer();
    assertSame(10, x);
    assertSame(20, y);
    assertSame(30, z);
    assertTrue(innerReturnCalled, 'Inner iterator return() should be called');
    assertTrue(outerReturnCalled, 'Outer iterator return() should be called');
}

// Array Destructuring in Assignment Context
{
    let returnCalled = false;

    class AssignMatcher {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [100, 200];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    let a, b;
    AssignMatcher(a, b) = new AssignMatcher();

    assertSame(100, a);
    assertSame(200, b);
    assertTrue(returnCalled, 'Iterator return() should be called in assignment');
}

// Iterator Closing in for-of Loop
{
    let iterationCount = 0;
    let returnCalled = false;

    class ForOfMatcher {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1, 2];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const items = [new ForOfMatcher(), new ForOfMatcher()];

    for (const ForOfMatcher(x, y) of items) {
        iterationCount++;
        assertTrue(x === 1 || x === 1);
        assertTrue(y === 2 || y === 2);
    }

    assertSame(2, iterationCount);
    assertTrue(returnCalled, 'Iterator return() should be called in for-of');
}

// Iterator Closing with Rest Element in Pattern
{
    let returnCalled = false;

    class RestMatcher {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [1, 2, 3, 4, 5];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    const RestMatcher(a, b, ...rest) = new RestMatcher();
    assertSame(1, a);
    assertSame(2, b);
    assertSameContent([3, 4, 5], rest);
    // assertTrue(returnCalled, 'Iterator return() should be called with rest element');
}

// Multiple Errors: next() throws after partial iteration
{
    let returnCalled = false;

    class PartialThenThrow {
        static [Symbol.customMatcher]() {
            let count = 0;
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    count++;
                    if (count === 1) {
                        return { value: 42, done: false };
                    }
                    throw new Error("second next() fails");
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    let caught = false;
    try {
        const PartialThenThrow(a, b) = new PartialThenThrow();
        fail('Should have thrown error from second next()');
    } catch (e) {
        caught = true;
        assertSame("second next() fails", e.message);
        // assertTrue(returnCalled, 'Iterator return() should be called on error');
    }
    assertTrue(caught, 'Exception should have been thrown');
}

// Iterator Closing in Function Parameters
{
    let returnCalled = false;

    class ParamMatcher {
        static [Symbol.customMatcher]() {
            let i = 0;
            const values = [5, 10];
            return {
                [Symbol.iterator]() {
                    return this;
                },
                next() {
                    if (i < values.length) {
                        return { value: values[i++], done: false };
                    }
                    return { done: true };
                },
                return() {
                    returnCalled = true;
                    return { done: true };
                }
            };
        }
    }

    function testFunc(ParamMatcher(x, y)) {
        assertSame(5, x);
        assertSame(10, y);
    }

    testFunc(new ParamMatcher());
    assertTrue(returnCalled, 'Iterator return() should be called in function parameters');
}

