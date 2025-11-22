/**
 * @option js.extractors
 *
 * https://github.com/tc39/proposal-extractors/issues/34
 */

load('../assert.js');

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
    assertTrue(sideEffectHappened);
    assertTrue(error instanceof ReferenceError);
}
