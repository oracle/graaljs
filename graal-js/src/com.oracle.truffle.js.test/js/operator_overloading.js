/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Operator overloading proposal.
 *
 * @option operator-overloading=true
 */

load('assert.js');

const ScalarOperators = Operators({
    "+"(a, b) {
        return new Scalar(a.value + b.value);
    },
    "-"(a, b) {
        return new Scalar(a.value - b.value);
    },
    "*"(a, b) {
        return new Scalar(a.value * b.value);
    },
    "/"(a, b) {
        return new Scalar(a.value / b.value);
    },
    "%"(a, b) {
        return new Scalar(a.value % b.value);
    },
    "**"(a, b) {
        return new Scalar(a.value ** b.value);
    },
    "&"(a, b) {
        return new Scalar(a.value & b.value);
    },
    "|"(a, b) {
        return new Scalar(a.value | b.value);
    },
    "^"(a, b) {
        return new Scalar(a.value ^ b.value);
    },
    "<<"(a, b) {
        return new Scalar(a.value << b.value);
    },
    ">>"(a, b) {
        return new Scalar(a.value >> b.value);
    },
    ">>>"(a, b) {
        return new Scalar(a.value >>> b.value);
    },
    "=="(a, b) {
        return a.value == b.value;
    },
    "<"(a, b) {
        return a.value < b.value;
    },
    "pos"(a) {
        return new Scalar(+a.value);
    },
    "neg"(a) {
        return new Scalar(-a.value);
    },
    "++"(a) {
        return new Scalar(a.value + 1);
    },
    "--"(a) {
        return new Scalar(a.value - 1);
    },
    "~"(a) {
        return new Scalar(~a.value);
    }
}, {
    left: Number,
    "=="(a, b) {
        return a == b.value;
    },
    "<"(a, b) {
        return a < b.value;
    }
}, {
    left: BigInt,
    "=="(a, b) {
        return a == b.value;
    },
    "<"(a, b) {
        return a < b.value;
    }
}, {
    right: Number,
    "=="(a, b) {
        return a.value == b;
    },
    "<"(a, b) {
        return a.value < b;
    }
}, {
    right: BigInt,
    "=="(a, b) {
        return a.value == b;
    },
    "<"(a, b) {
        return a.value < b;
    }
});

class Scalar extends ScalarOperators {
    value;

    constructor(value) {
        super();
        this.value = value;
    }

    toString() {
        return String(this.value);
    }
}

function S(x) {
    return new Scalar(x);
}


const VectorOperators = Operators({
    "+"(a, b) {
        return new Vector(a.contents.map((e, i) => e + b.contents[i]));
    },
    "-"(a, b) {
        return new Vector(a.contents.map((e, i) => e - b.contents[i]));
    },
    "=="(a, b) {
        if (a.contents.length != b.contents.length) {
            return false;
        }
        for (let i = 0; i < a.contents.length; i++) {
            if (a.contents[i] != b.contents[i]) {
                return false;
            }
        }
        return true;
    },
    "<"(a, b) {
        if (a.contents.length < b.contents.length) {
            return true;
        } else if (a.contents.length > b.contents.length) {
            return false;
        }
        for (let i = 0; i < a.contents.length; i++) {
            if (a.contents[i] < b.contents[i]) {
                return true;
            } else if (a.contents[i] > b.contents[i]) {
                return false;
            }
        }
        return false;
    },
    "pos"(a) {
        return new Vector(a.contents.slice());
    },
    "neg"(a) {
        return new Vector(a.contents.map(e => -e));
    }
}, {
    right: Number,
    "*"(v, x) {
        return new Vector(v.contents.map(e => e * x));
    }
}, {
    right: Scalar,
    "*"(v, x) {
        return new Vector(v.contents.map(e => e * x.value));
    }
}, {
    left: Number,
    "*"(x, v) {
        return new Vector(v.contents.map(e => e * x));
    }
}, {
    left: Scalar,
    "*"(x, v) {
        return new Vector(v.contents.map(e => e * x.value));
    }
});

class Vector extends VectorOperators {
    contents;

    constructor(contents) {
        super();
        this.contents = contents;
    }

    toString() {
        return "<" + this.contents + ">";
    }
}

function V(...args) {
    return new Vector([...args]);
}


const CustomStringOperators = Operators({
    "=="(a, b) {
        return a.string == b.string;
    },
    "<"(a, b) {
        return a.string < b.string;
    }
}, {
    left: String,
    "=="(a, b) {
        return a == b.string;
    },
    "<"(a, b) {
        return a < b.string;
    }
}, {
    right: String,
    "=="(a, b) {
        return a.string == b;
    },
    "<"(a, b) {
        return a.string < b;
    }
});

class CustomString extends CustomStringOperators {
    string;

    constructor(string) {
        super();
        this.string = string;
    }

    toString() {
        return this.string;
    }
}

function STR(string) {
    return new CustomString(string);
}


// Basic tests on vectors: addition, subtraction, unary negation, equality
assertEqual(V(1, 2, 3) + V(2, 3, 4), V(3, 5, 7));
assertEqual(V(1, 2, 3) - V(2, 3, 4), V(-1, -1, -1));
assertTrue(-V(1, 2, 3) == V(-1, -2, -3))

// Update operators should work too
let A = V(1, 2, 3);
A += V(1, 1, 1);
assertEqual(A, V(2, 3, 4));

// Mixed type operators: multiplying a vector by a scalar (either a Number or a custom object)
assertEqual(2 * V(1, 2, 3), V(2, 4, 6));
assertEqual(V(1, 2, 3) * 2, V(2, 4, 6));
assertEqual(S(2) * V(1, 2, 3), V(2, 4, 6));
assertEqual(V(1, 2, 3) * S(2), V(2, 4, 6));


// Testing the prefix and suffix increment and decrement operators
let x = S(3);

assertEqual(x++, S(3));
assertEqual(x, S(4));

assertEqual(++x, S(5));
assertEqual(x, S(5));

assertEqual(x--, S(5));
assertEqual(x, S(4));

assertEqual(--x, S(3));
assertEqual(x, S(3));


// Testing comparison operators
assertTrue(S(1) < S(2));
assertTrue(S(1) <= S(2));
assertTrue(S(2) > S(1));
assertTrue(S(2) >= S(1));

assertTrue(S(1) < 2);
assertTrue(S(1) <= 2);
assertTrue(S(2) > 1);
assertTrue(S(2) >= 1);

assertTrue(1 < S(2));
assertTrue(1 <= S(2));
assertTrue(2 > S(1));
assertTrue(2 >= S(1));

assertTrue(S(1) == S(1));
assertTrue(S(1) == 1);
assertTrue(1 == S(1));
assertTrue(S(1) != S(2));
assertTrue(S(1) != 2);
assertTrue(1 != S(2));


// Testing all the operators
assertEqual(S(1) + S(2), S(1 + 2));
assertEqual(S(1) - S(2), S(1 - 2));
assertEqual(S(1) * S(2), S(1 * 2));
assertEqual(S(1) / S(2), S(1 / 2));
assertEqual(S(1) % S(2), S(1 % 2));
assertEqual(S(1) ** S(2), S(1 ** 2));
assertEqual(S(1) & S(2), S(1 & 2));
assertEqual(S(1) | S(2), S(1 | 2));
assertEqual(S(1) ^ S(2), S(1 ^ 2));
assertEqual(S(1) << S(2), S(1 << 2));
assertEqual(S(1) >> S(2), S(1 >> 2));
assertEqual(S(1) >>> S(2), S(1 >>> 2));
assertEqual(S(1) == S(2), 1 == 2);
assertEqual(S(1) != S(2), 1 != 2);
assertEqual(S(1) < S(2), 1 < 2);
assertEqual(S(1) <= S(2), 1 <= 2);
assertEqual(S(1) > S(2), 1 > 2);
assertEqual(S(1) >= S(2), 1 >= 2);
assertEqual(+S(1), S(+1));
assertEqual(-S(1), S(-1));


// Testing BigInt overloads
assertTrue(S(1) == BigInt(1));
assertTrue(BigInt(1) == S(1));
assertTrue(S(1) < BigInt(2));
assertTrue(BigInt(1) < S(2));


// Testing String overloads
assertTrue(STR("hello") == "hello");
assertTrue("hello" == STR("hello"));
assertTrue("a" < STR("b"));
assertTrue(STR("a") < "b");


// According to the current wording of the operator overloading proposal, adding a string to an
// object should always result in a string
assertSame(S(1) + "a", "1a");
assertSame("a" + S(1), "a1");
assertSame(V(1, 2, 3) + "a", "<1,2,3>a");
assertSame("a" + V(1, 2, 3), "a<1,2,3>");
assertSame(STR("a") + "b", "ab");
assertSame("a" + STR("b"), "ab");


// Using undefined operators or type combinations should result in type errors
assertThrows(() => V(1, 2, 3) / V(2, 3, 4), TypeError);
assertThrows(() => 1 + V(1, 2, 3), TypeError);
assertThrows(() => S(1) + V(1, 2, 3), TypeError);
assertThrows(() => V(1, 2, 3) + STR("a"), TypeError);
assertThrows(() => { let v = V(1, 2, 3); v++; }, TypeError);
assertThrows(() => { let v = V(1, 2, 3); v--; }, TypeError);


// Handle null, undefined and non-numeric primitives when dispatching operators that use ToOperand
// (ToPrimitive) internally.
// The cases below are not covered explicitly by the operator overloading proposal, but this
// behavior implied by the proposal spec.

function illegalValueThrowsTypeError(illegalValue) {
    assertThrows(() => S(1) + illegalValue, TypeError);
    // For equality checks, a missing operator is interpreted as a negative result.
    assertFalse(S(1) == illegalValue);
    assertTrue(S(1) != illegalValue);
    assertThrows(() => S(1) < illegalValue, TypeError);
    assertThrows(() => S(1) <= illegalValue, TypeError);
    assertThrows(() => S(1) > illegalValue, TypeError);
    assertThrows(() => S(1) >= illegalValue, TypeError);
}

illegalValueThrowsTypeError(undefined);
illegalValueThrowsTypeError(null);
illegalValueThrowsTypeError(false);
illegalValueThrowsTypeError(true);
illegalValueThrowsTypeError(Symbol("foo"));


// The Operators function can reject junk input
assertThrows(() => Operators(), TypeError);
assertThrows(() => Operators(42), TypeError);
assertThrows(() => Operators({}, 42), TypeError);
assertThrows(() => Operators({}, {}), TypeError);
assertThrows(() => Operators({}, {left: Number, right: Number}), TypeError);
assertThrows(() => Operators({}, {left: 42}), TypeError);
assertThrows(() => Operators({}, {right: 42}), TypeError);
assertThrows(() => Operators({"+": 42}), TypeError);
assertThrows(() => Operators({}, {left: Number, "+": 42}), TypeError);
assertThrows(() => Operators({}, {right: Number, "+": 42}), TypeError);
assertThrows(() => Operators({open: ["foo"]}), TypeError);


// Operators declared as not open cannot be overloaded
const opA = Operators({open: ["+"]});
class classA extends opA {}
assertThrows(() => Operators({}, {left: classA, "-": (a, b) => a - b}), TypeError);


// Numeric operators on Strings cannot be overloaded
for (const numericOperator of ["-", "*", "/", "%", "**", "&", "|", "^", "<<", ">>", ">>>"]) {
    assertThrows(() => Operators({}, {left: String, [numericOperator]: (a, b) => a + b}), TypeError);
    assertThrows(() => Operators({}, {right: String, [numericOperator]: (a, b) => a + b}), TypeError);
}
