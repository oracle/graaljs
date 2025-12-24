/**
 * @option js.extractors
 */

/**
 * Error Handling
 *
 * Tests error scenarios and side effects in extractor patterns:
 * - Call expression assignment errors with side effects
 * - Error propagation from custom matchers
 */

load('../assert.js');

// Call Assignment Error with Side Effect
// https://github.com/tc39/proposal-extractors/issues/34
{
    let sideEffectHappened = false;

    function f() {
        sideEffectHappened = true;
    }

    try {
        f() = 1;
    } catch (error) {
        if (!sideEffectHappened) {
            console.log(error);
        }
        assertTrue(sideEffectHappened, 'Side effect should happen before ReferenceError');
        assertTrue(error instanceof ReferenceError, 'Should throw ReferenceError');
    }
}

// CustomMatcher Error - Returns Non-Object
{
    class ReturnsNull {
        static [Symbol.customMatcher]() {
            return null;
        }
    }

    try {
        const ReturnsNull(x) = new ReturnsNull();
        fail('Should have thrown TypeError');
    } catch (e) {
        assertTrue(e instanceof TypeError, 'Should throw TypeError for null return');
    }
}

// CustomMatcher Error - Throws During Execution
{
    class ThrowsError {
        static [Symbol.customMatcher]() {
            throw new Error("Custom matcher error");
        }
    }

    try {
        const ThrowsError(x) = new ThrowsError();
        fail('Should have propagated the error');
    } catch (e) {
        assertSame("Custom matcher error", e.message);
    }
}

// CustomMatcher Error - Iterator Error
{
    class BadIterator {
        static [Symbol.customMatcher]() {
            return {
                [Symbol.iterator]() {
                    throw new Error("Iterator creation failed");
                }
            };
        }
    }

    try {
        const BadIterator(x) = new BadIterator();
        fail('Should have thrown error from iterator');
    } catch (e) {
        assertSame("Iterator creation failed", e.message);
    }
}

