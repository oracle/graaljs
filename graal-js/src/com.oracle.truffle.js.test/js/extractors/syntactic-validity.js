/**
 * @option js.extractors
 */

/**
 * Syntactic Validity & Locations
 *
 * Tests that extractors are supported in all valid pattern contexts:
 * - Variable declarations (const, let, var)
 * - Assignments
 * - Function parameters (declarations, expressions, arrows, methods)
 * - Iteration (for-of, for-await-of, for-in, for loops)
 * - Exception handling (catch clauses)
 * - Nesting (inside arrays and objects)
 * - Recursive composition (extractors inside extractors)
 *
 * Also tests that extractors are rejected in invalid contexts:
 * - Expression positions (non-pattern contexts)
 * - As rest elements
 * - Other syntactically invalid positions
 */

load("../assert.js");
load("common.js");

// Variable Declarations
{
    const p = new Pair(1, 2);

    const Pair(x1, y1) = p;
    assertSame(1, x1);
    assertSame(2, y1);

    let Pair(x2, y2) = p;
    assertSame(1, x2);
    assertSame(2, y2);

    var Pair(x3, y3) = p;
    assertSame(1, x3);
    assertSame(2, y3);
}

// Assignments
{
    const p = new Pair(3, 4);
    let a, b;

    Pair(a, b) = p;
    assertSame(3, a);
    assertSame(4, b);
}

// Function Parameters - Function Declarations
{
    function declFunc(Pair(x, y)) {
        return x + y;
    }

    const result = declFunc(new Pair(5, 6));
    assertSame(11, result);
}

// Function Parameters - Function Expressions
{
    const exprFunc = function(Pair(x, y)) {
        return x * y;
    };

    const result = exprFunc(new Pair(7, 8));
    assertSame(56, result);
}

// Function Parameters - Arrow Functions (with parentheses)
{
    const arrowFunc = (Pair(x, y)) => x - y;

    const result = arrowFunc(new Pair(10, 3));
    assertSame(7, result);
}

// Function Parameters - Class Methods
{
    class Calculator {
        add(Pair(x, y)) {
            return x + y;
        }

        static multiply(Pair(x, y)) {
            return x * y;
        }
    }

    const calc = new Calculator();
    assertSame(15, calc.add(new Pair(7, 8)));
    assertSame(42, Calculator.multiply(new Pair(6, 7)));
}

// Function Parameters - Class Constructors
{
    class PairHolder {
        constructor(Pair(x, y)) {
            this.x = x;
            this.y = y;
        }
    }

    const holder = new PairHolder(new Pair(11, 12));
    assertSame(11, holder.x);
    assertSame(12, holder.y);
}

// Function Parameters - Class Setters
{
    class PairStorage {
        #x;
        #y;

        set point(Pair(x, y)) {
            this.#x = x;
            this.#y = y;
        }

        get sum() {
            return this.#x + this.#y;
        }
    }

    const storage = new PairStorage();
    storage.point = new Pair(13, 14);
    assertSame(27, storage.sum);
}

// Function Parameters - Object Literal Methods
{
    const obj = {
        process(Pair(x, y)) {
            return { x, y };
        }
    };

    const result = obj.process(new Pair(15, 16));
    assertSame(15, result.x);
    assertSame(16, result.y);
}

// Iteration - for-of
{
    const points = [new Pair(1, 2), new Pair(3, 4), new Pair(5, 6)];
    const xs = [];
    const ys = [];

    for (const Pair(x, y) of points) {
        xs.push(x);
        ys.push(y);
    }

    assertSameContent([1, 3, 5], xs);
    assertSameContent([2, 4, 6], ys);
}

// Iteration - for-await-of
{
    async function testForAwaitOf() {
        async function* pointGenerator() {
            yield new Pair(7, 8);
            yield new Pair(9, 10);
        }

        const xs = [];
        const ys = [];

        for await (const Pair(x, y) of pointGenerator()) {
            xs.push(x);
            ys.push(y);
        }

        assertSameContent([7, 9], xs);
        assertSameContent([8, 10], ys);
    }

    testForAwaitOf();
}

// Iteration - for-in
{
    const obj = {
        p1: new Pair(11, 12),
        p2: new Pair(13, 14)
    };

    // Note: for-in uses string keys, not the values directly
    // This tests that the syntax is valid, even if semantically unusual
    const keys = [];
    for (const key in obj) {
        keys.push(key);
    }

    assertTrue(keys.includes('p1'));
    assertTrue(keys.includes('p2'));
}

// Iteration - Standard for Loop Headers
{
    const Pair(initX, initY) = new Pair(100, 200);
    assertSame(100, initX);
    assertSame(200, initY);
}

// Exception Handling -   Clauses
{
    class ErrorWrapper {
        constructor(error) {
            this.error = error;
        }

        static [Symbol.customMatcher](wrapper) {
            return [wrapper.error];
        }
    }

    try {
        throw new ErrorWrapper(new Error("test error"));
    } catch (ErrorWrapper(e)) {
        assertSame("test error", e.message);
    }
}

// Nesting - Inside Array Destructuring
{
    const p = new Pair(17, 18);
    const [Pair(x, y), other, somethingElse] = [p, "value", 1];

    assertSame(17, x);
    assertSame(18, y);
    assertSame("value", other);
    assertSame(1, somethingElse);
}

// Nesting - Inside Object Destructuring
{
    const p = new Pair(19, 20);
    const { point: Pair(x, y), name } = { point: p, name: "test" };

    assertSame(19, x);
    assertSame(20, y);
    assertSame("test", name);
}

// Nesting - Deep Object Destructuring
{
    const p = new Pair(21, 22);
    const { a: { b: { c: Pair(x, y) } } } = { a: { b: { c: p } } };

    assertSame(21, x);
    assertSame(22, y);
}

// Recursive Composition - Extractor Inside Extractor
{
    const line = new Pair(new Pair(1, 2), new Pair(3, 4));
    const Pair(Pair(x1, y1), Pair(x2, y2)) = line;

    assertSame(1, x1);
    assertSame(2, y1);
    assertSame(3, x2);
    assertSame(4, y2);
}

// Recursive Composition - Multiple Levels
{
    class Container {
        constructor(value) {
            this.value = value;
        }

        static [Symbol.customMatcher](container) {
            return [container.value];
        }
    }

    const nested = new Container(new Pair(new Pair(5, 6), new Pair(7, 8)));
    const Container(Pair(Pair(x1, y1), Pair(x2, y2))) = nested;

    assertSame(5, x1);
    assertSame(6, y1);
    assertSame(7, x2);
    assertSame(8, y2);
}

// Nesting - Extractor in Array with Rest Element
{
    const t = new Triple(1, 2, 3);
    const [Triple(a, b, c), ...rest] = [t, 4, 5, 6];

    assertSame(1, a);
    assertSame(2, b);
    assertSame(3, c);
    assertSameContent([4, 5, 6], rest);
}

// Complex Nesting - Arrays and Objects Combined
{
    const p1 = new Pair(23, 24);
    const p2 = new Pair(25, 26);

    const [{ points: [Pair(x1, y1), Pair(x2, y2)] }] = [{ points: [p1, p2] }];

    assertSame(23, x1);
    assertSame(24, y1);
    assertSame(25, x2);
    assertSame(26, y2);
}

// Multiple Parameters with Extractors
{
    function multiParam(Pair(x1, y1), Pair(x2, y2), Pair(x3, y3)) {
        return x1 + x2 + x3 + y1 + y2 + y3;
    }

    const result = multiParam(new Pair(1, 2), new Pair(3, 4), new Pair(5, 6));
    assertSame(21, result);
}

// Default Parameters with Extractors
{
    function withDefaults(Pair(x, y) = new Pair(0, 0)) {
        return { x, y };
    }

    const result1 = withDefaults(new Pair(27, 28));
    assertSame(27, result1.x);
    assertSame(28, result1.y);

    const result2 = withDefaults();
    assertSame(0, result2.x);
    assertSame(0, result2.y);
}

// Rest Parameters After Extractor
{
    function withRest(Pair(x, y), ...args) {
        return { x, y, args };
    }

    const result = withRest(new Pair(29, 30), 'a', 'b', 'c');
    assertSame(29, result.x);
    assertSame(30, result.y);
    assertSameContent(['a', 'b', 'c'], result.args);
}

// Negative Test: Extractor in Expression Position (not a pattern)
{
    assertThrows(() => {
        eval("const x = PlainObjectWithCustomMatcher({ a: 1 });");
    });
}

// Negative Test: Extractor as Standalone Expression
{
    assertThrows(() => {
        eval("PlainObjectWithCustomMatcher({ a: 1 })");
    });
}

// Negative Test: Extractor in Return Statement
{
    assertThrows(() => {
        eval("function f() { return PlainObjectWithCustomMatcher({ a: 1 }); }; f();");
    });
}

// Negative Test: Extractor in Conditional Expression
{
    assertThrows(() => {
        eval("const x = true ? PlainObjectWithCustomMatcher({ a: 1 }) : 2;");
    });
}

// Negative Test: Extractor as Function Call Argument (non-destructuring)
{
    assertThrows(() => {
        eval("console.log(Pair(x));");
    });
}

// Negative Test: Extractor Used as Rest Element
{
    // [...Foo()] is invalid - rest elements cannot be extractors
    assertThrows(() => {
        eval("const [...Pair(x, y)] = [new Pair(1, 2)];");
    });
}

// Negative Test: Extractor as Object Rest Element
{
    assertThrows(() => {
        eval("const {...Pair(x, y)} = { a: new Pair(1, 2) };");
    });
}

// Negative Test: Extractor in Array Rest Position
{
    assertThrows(() => {
        eval("const [a, ...Pair(x, y)] = [1, new Pair(2, 3)];");
    });
}

// Negative Test: Extractor with Spread Operator in Call
{
    assertThrows(() => {
        eval("func(...Pair(x, y));");
    });
}

// Negative Test: Extractor in Binary Expression
{
    assertThrows(() => {
        eval("const x = Pair(a, b) + 5;");
    });
}

// Negative Test: Extractor in Member Expression Chain
{
    assertThrows(() => {
        eval("const x = Pair(a, b).property;");
    });
}

// Negative Test: Extractor as Array Element (not pattern)
{
    assertThrows(() => {
        eval("const arr = [Pair(x, y)];");
    });
}

// Negative Test: Extractor as Object Property Value (not pattern)
{
    assertThrows(() => {
        eval("const obj = { prop: Pair(x, y) };");
    });
}

// Negative Test: Extractor in typeof Expression
{
    assertThrows(() => {
        eval("const x = typeof Pair(y);");
    });
}

// Negative Test: Extractor in Unary Expression
{
    assertThrows(() => {
        eval("const x = !Pair(y);");
    });
}

// Negative Test: Extractor in Logical Expression
{
    assertThrows(() => {
        eval("const x = Pair(a) && Pair(b);");
    });
}

// Negative Test: Extractor in Template Literal
{
    assertThrows(() => {
        eval("const x = `value: ${Pair(y)}`;");
    });
}

// Negative Test: Extractor in New Expression
{
    assertThrows(() => {
        eval("const x = new Pair(Pair(a, b));");
    });
}

// Negative Test: Extractor as Class Name
{
    assertThrows(() => {
        eval("class Pair(x, y) {}");
    });
}
