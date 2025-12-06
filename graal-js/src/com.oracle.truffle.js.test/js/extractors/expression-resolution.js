/**
 * @option js.extractors
 */

/**
 * Extractor Expression Resolution
 *
 * Tests different forms of extractor expressions:
 * - Simple identifiers
 * - Member expressions (qualified names)
 * - Contextual references (this, super)
 * - Dynamic access (computed properties)
 */

load('../assert.js');

// Simple Identifier Reference
{
    class Simple {
        constructor(value) {
            this.value = value;
        }

        static [Symbol.customMatcher](obj) {
            return [obj.value];
        }
    }

    const Simple(x) = new Simple(42);
    assertSame(42, x);
}

// Member Expression - Qualified Name
{
    const namespace = {
        Point: class {
            constructor(x, y) {
                this.x = x;
                this.y = y;
            }

            static [Symbol.customMatcher](point) {
                return [point.x, point.y];
            }
        }
    };

    const p = new namespace.Point(10, 20);
    const namespace.Point(x, y) = p;

    assertSame(10, x);
    assertSame(20, y);
}

// Member Expression - Nested Qualified Name
{
    const deeply = {
        nested: {
            module: {
                Extractor: class {
                    constructor(value) {
                        this.value = value;
                    }

                    static [Symbol.customMatcher](obj) {
                        return [obj.value];
                    }
                }
            }
        }
    };

    const obj = new deeply.nested.module.Extractor("deep");
    const deeply.nested.module.Extractor(result) = obj;

    assertSame("deep", result);
}

// Contextual Reference - this
{
    class Container {
        constructor() {
            this.Extractor = class {
                constructor(value) {
                    this.value = value;
                }

                static [Symbol.customMatcher](obj) {
                    return [obj.value];
                }
            };
        }

        extract(obj) {
            const this.Extractor(value) = obj;
            return value;
        }
    }

    const container = new Container();
    const obj = new container.Extractor("test");
    const result = container.extract(obj);

    assertSame("test", result);
}

// Contextual Reference - super (in class context)
{
    class Base {
        static Extractor = class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        };
    }

    class Derived extends Base {
        static extract(obj) {
            const super.Extractor(value) = obj;
            return value;
        }
    }

    const obj = new Base.Extractor("super");
    const result = Derived.extract(obj);

    assertSame("super", result);
}

// Dynamic Access - Computed Property (Array Index)
{
    const extractors = [
        class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        }
    ];

    const obj = new extractors[0]("indexed");
    const extractors[0](result) = obj;

    assertSame("indexed", result);
}

// Dynamic Access - Computed Property (String Key)
{
    const registry = {
        "custom-extractor": class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        }
    };

    const obj = new registry["custom-extractor"]("computed");
    const registry["custom-extractor"](result) = obj;

    assertSame("computed", result);
}

// Dynamic Access - Computed Property (Variable)
{
    const extractors = {
        Point: class {
            constructor(x, y) {
                this.x = x;
                this.y = y;
            }

            static [Symbol.customMatcher](point) {
                return [point.x, point.y];
            }
        },
        Triple: class {
            constructor(a, b, c) {
                this.a = a;
                this.b = b;
                this.c = c;
            }

            static [Symbol.customMatcher](triple) {
                return [triple.a, triple.b, triple.c];
            }
        }
    };

    const key = "Point";
    const p = new extractors[key](5, 10);
    const extractors[key](x, y) = p;

    assertSame(5, x);
    assertSame(10, y);
}

// Dynamic Access - Expression as Property
{
    const extractors = {
        ext1: class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        }
    };

    function getKey() {
        return "ext" + 1;
    }

    const obj = new extractors[getKey()]("dynamic");
    const extractors[getKey()](result) = obj;

    assertSame("dynamic", result);
}

// Mixed - Member Expression with Computed Property
{
    const module = {
        extractors: {
            0: class {
                constructor(value) {
                    this.value = value;
                }

                static [Symbol.customMatcher](obj) {
                    return [obj.value];
                }
            }
        }
    };

    const obj = new module.extractors[0]("mixed");
    const module.extractors[0](result) = obj;

    assertSame("mixed", result);
}

// Member Expression in Function Parameter
{
    const lib = {
        Point: class {
            constructor(x, y) {
                this.x = x;
                this.y = y;
            }

            static [Symbol.customMatcher](point) {
                return [point.x, point.y];
            }
        }
    };

    function process(lib.Point(x, y)) {
        return x + y;
    }

    const result = process(new lib.Point(15, 25));
    assertSame(40, result);
}

// Computed Property in Function Parameter
{
    const extractors = [
        class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        }
    ];

    function extract(extractors[0](value)) {
        return value * 2;
    }

    const result = extract(new extractors[0](21));
    assertSame(42, result);
}

// this Reference in Object Method
{
    const obj = {
        Extractor: class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        },

        extract(arg) {
            const this.Extractor(value) = arg;
            return value.toUpperCase();
        }
    };

    const arg = new obj.Extractor("hello");
    const result = obj.extract(arg);

    assertSame("HELLO", result);
}

// Complex Member Expression Chain
{
    const a = {
        b: {
            c: {
                d: {
                    Extractor: class {
                        constructor(value) {
                            this.value = value;
                        }

                        static [Symbol.customMatcher](obj) {
                            return [obj.value];
                        }
                    }
                }
            }
        }
    };

    const obj = new a.b.c.d.Extractor("chain");
    const a.b.c.d.Extractor(result) = obj;

    assertSame("chain", result);
}

// Symbol as Computed Property
{
    const sym = Symbol('extractor');
    const registry = {
        [sym]: class {
            constructor(value) {
                this.value = value;
            }

            static [Symbol.customMatcher](obj) {
                return [obj.value];
            }
        }
    };

    const obj = new registry[sym]("symbol");
    const registry[sym](result) = obj;

    assertSame("symbol", result);
}

