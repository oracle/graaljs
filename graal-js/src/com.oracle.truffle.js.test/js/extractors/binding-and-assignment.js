/**
 * @option js.extractors
 */

/**
 * Argument Binding and Assignment
 *
 * Tests that:
 * - In binding contexts (variable declarations), arguments create new bindings
 * - In assignment contexts, arguments must be valid LHS targets
 * - Assignment targets can include property access, array indexing, etc.
 */

load('../assert.js');
load('common.js');

// Binding Context - const Creates New Bindings
{
    const p = new Pair(1, 2);
    const Pair(x, y) = p;

    // x and y are new bindings created by the declaration
    assertSame(1, x);
    assertSame(2, y);

    // They are independent variables
    const x2 = x + 10;
    const y2 = y + 20;
    assertSame(11, x2);
    assertSame(22, y2);
}

// Binding Context - let Creates New Bindings
{
    const p = new Pair(3, 4);
    let Pair(x, y) = p;

    assertSame(3, x);
    assertSame(4, y);

    // Variables can be reassigned (mutable bindings)
    x = x * 2;
    y = y * 2;
    assertSame(6, x);
    assertSame(8, y);
}

// Binding Context - var Creates New Bindings
{
    const p = new Pair(5, 6);
    var Pair(varX, varY) = p;

    assertSame(5, varX);
    assertSame(6, varY);
}

// Binding Context - Function Parameters Create New Bindings
{
    function test(Pair(x, y)) {
        // x and y are parameter bindings
        assertSame(7, x);
        assertSame(8, y);

        // Can be modified within the function
        x = x + 1;
        y = y + 1;
        return { x, y };
    }

    const result = test(new Pair(7, 8));
    assertSame(8, result.x);
    assertSame(9, result.y);
}

// Assignment Context - Simple Variables
{
    const p = new Pair(9, 10);
    let x, y;

    // Assignment to existing variables
    Pair(x, y) = p;

    assertSame(9, x);
    assertSame(10, y);
}

// Assignment Context - Object Properties
{
    const p = new Pair(11, 12);
    const obj = {};

    // Assignment to object properties
    Pair(obj.x, obj.y) = p;

    assertSame(11, obj.x);
    assertSame(12, obj.y);
}

// Assignment Context - Nested Object Properties
{
    const p = new Pair(13, 14);
    const obj = { nested: {} };

    Pair(obj.nested.x, obj.nested.y) = p;

    assertSame(13, obj.nested.x);
    assertSame(14, obj.nested.y);
}

// Assignment Context - Array Elements
{
    const p = new Pair(15, 16);
    const arr = [];

    // Assignment to array indices
    Pair(arr[0], arr[1]) = p;

    assertSame(15, arr[0]);
    assertSame(16, arr[1]);
}

// Assignment Context - Mixed Array Indices
{
    const p = new Pair(17, 18);
    const arr = new Array(10);

    Pair(arr[5], arr[9]) = p;

    assertSame(17, arr[5]);
    assertSame(18, arr[9]);
}

// Assignment Context - Computed Property Access
{
    const t = new Triple(19, 20, 21);
    const obj = {};
    const keys = ['a', 'b', 'c'];

    Triple(obj[keys[0]], obj[keys[1]], obj[keys[2]]) = t;

    assertSame(19, obj.a);
    assertSame(20, obj.b);
    assertSame(21, obj.c);
}

// Assignment Context - Mixed Targets
{
    const t = new Triple(22, 23, "24");
    let x;
    const obj = {};
    const arr = [];

    // Mix of variable, object property, and array element
    Triple(x, obj.prop, arr[0]) = t;

    assertSame(22, x);
    assertSame(23, obj.prop);
    assertSame("24", arr[0]);
}

// Assignment Context - Nested Structures
{
    const t = new Triple(24, 25, 26);
    const state = { data: {}, list: [] };

    Triple(state.data.a, state.data.b, state.list[0]) = t;

    assertSame(24, state.data.a);
    assertSame(25, state.data.b);
    assertSame(26, state.list[0]);
}

// Assignment Context - this Properties in Method
{
    class Storage {
        store(p) {
            Pair(this.x, this.y) = p;
        }

        get() {
            return { x: this.x, y: this.y };
        }
    }

    const storage = new Storage();
    storage.store(new Pair(27, 28));

    const result = storage.get();
    assertSame(27, result.x);
    assertSame(28, result.y);
}

// Assignment Context - Global Object Properties
{
    const p = new Pair(29, 30);

    Pair(globalThis.testX, globalThis.testY) = p;

    assertSame(29, globalThis.testX);
    assertSame(30, globalThis.testY);

    // Cleanup
    delete globalThis.testX;
    delete globalThis.testY;
}

// Assignment Context - Complex LHS Expressions
{
    const p = new Pair(31, 32);
    const obj = { items: [{}, {}, {}] };

    Pair(obj.items[1].x, obj.items[2].y) = p;

    assertSame(31, obj.items[1].x);
    assertSame(32, obj.items[2].y);
}

// Binding and Assignment - Default Values in Binding
{
    class Optional {
        constructor(value) {
            this.value = value;
        }

        static [Symbol.customMatcher](obj) {
            return [obj.value];
        }
    }

    // Default value is used when undefined
    const Optional(x = 100) = new Optional(undefined);
    assertSame(100, x);

    // Default value is not used when value is present
    const Optional(y = 100) = new Optional(50);
    assertSame(50, y);
}

// Assignment - Default Values in Assignment
{
    class Optional {
        constructor(value) {
            this.value = value;
        }

        static [Symbol.customMatcher](obj) {
            return [obj.value];
        }
    }

    let a, b;

    // Default value in assignment context
    Optional(a = 200) = new Optional(undefined);
    assertSame(200, a);

    Optional(b = 200) = new Optional(75);
    assertSame(75, b);
}

// Binding Context - Rest Pattern
{
    const t = new Triple(33, 34, 35);
    const Triple(first, ...rest) = t;

    assertSame(33, first);
    assertSameContent([34, 35], rest);
}

// Assignment Context - Rest Pattern
{
    const t = new Triple(36, 37, 38);
    let a, b;

    Triple(a, ...b) = t;

    assertSame(36, a);
    assertSameContent([37, 38], b);
}

// Assignment Context - Nested Extractors with Property Access
{
    class Container {
        constructor(value) {
            this.value = value;
        }

        static [Symbol.customMatcher](obj) {
            return [obj.value];
        }
    }

    const obj = {};
    const nested = new Container(new Pair(39, 40));

    Container(Pair(obj.x, obj.y)) = nested;

    assertSame(39, obj.x);
    assertSame(40, obj.y);
}

// Binding and Assignment - Scoping
{
    const p = new Pair(41, 42);

    // Binding creates new scope-local variables
    {
        const Pair(x, y) = p;
        assertSame(41, x);
        assertSame(42, y);
    }

    // x and y are not accessible here (would throw ReferenceError)
    assertThrows(() => x, ReferenceError);
    assertThrows(() => y, ReferenceError);
}

// Assignment - Preserves Existing Variables
{
    const p1 = new Pair(43, 44);
    const p2 = new Pair(45, 46);

    let x, y;

    Pair(x, y) = p1;
    assertSame(43, x);
    assertSame(44, y);

    // Assignment updates the same variables
    Pair(x, y) = p2;
    assertSame(45, x);
    assertSame(46, y);
}

// Assignment Context - Symbol Properties
{
    const p = new Pair(47, 48);
    const obj = {};
    const symX = Symbol('x');
    const symY = Symbol('y');

    Pair(obj[symX], obj[symY]) = p;

    assertSame(47, obj[symX]);
    assertSame(48, obj[symY]);
}

// Assignment Context - Dynamic Property Names
{
    const p = new Pair(49, 50);
    const obj = {};

    function getProp(name) {
        return 'prop_' + name;
    }

    Pair(obj[getProp('x')], obj[getProp('y')]) = p;

    assertSame(49, obj.prop_x);
    assertSame(50, obj.prop_y);
}

// Binding Context - Nested Destructuring with Extractors
{
    const p = new Pair(51, 52);
    const { wrapper: Pair(x, y) } = { wrapper: p };

    assertSame(51, x);
    assertSame(52, y);
}

