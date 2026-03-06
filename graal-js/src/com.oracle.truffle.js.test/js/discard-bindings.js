/**
 * Basic tests for Discard Bindings.
 * These tests are derived directly from the 
 *
 * @option js.discard-bindings
 */

load("assert.js");

function testObjectDestructuring() {
    const { z: void, ...obj1 } = { x: 1, y: 2, z: 3 };
    assertSame(1, obj1.x);
    assertSame(2, obj1.y);
    assertFalse('z' in obj1);
    assertThrows(() => z, ReferenceError);

    let obj2;
    ({ z: void, ...obj2 } = { x: 10, y: 20, z: 30 });
    assertSame(10, obj2.x);
    assertSame(20, obj2.y);
    assertFalse('z' in obj2);
    assertThrows(() => z, ReferenceError);
}

function testArrayDestructuring() {
    const [a, void, b] = [1, 2, 3];
    assertSame(1, a);
    assertSame(3, b);

    let x, y;
    ([x, void, void, y] = [10, 20, 30, 40]);
    assertSame(10, x);
    assertSame(40, y);
}

function testParameters() {
    const arr = ['a', 'b', 'c'];
    const indices = arr.map((void, i) => i);
    assertSameContent([0, 1, 2], indices);

    function discardFirst(void, second) {
        return second;
    }
    assertSame(42, discardFirst(99, 42));
    assertSame(undefined, discardFirst(42));
}

function testIteratorConsumption() {
    const iter = Iterator.from("0123456789");

    const [a, void, void] = iter;
    assertSame("0", a);

    const [b] = iter;
    assertSame("3", b);
}

function testDisallowedSyntax() {
    // Discard bindings are not supported at the top level of var/let/const declarations.
    assertThrows(() => eval('const void = 5;'), SyntaxError);
    assertThrows(() => eval('let void = 5;'), SyntaxError);
    assertThrows(() => eval('var void = 5;'), SyntaxError);

    // A bare void assignment is disallowed because void is not part of the AssignmentPattern refinement.
    assertThrows(() => eval('void = 5;'), SyntaxError);
}

const tests = [
    testObjectDestructuring,
    testArrayDestructuring,
    testParameters,
    testIteratorConsumption,
    testDisallowedSyntax
];

for (const test of tests) {
    test();
}
