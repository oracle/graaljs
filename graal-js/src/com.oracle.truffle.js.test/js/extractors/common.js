/**
 * Common test classes and utilities for extractor tests.
 */

class Pair {
    constructor(first, second) {
        this.first = first;
        this.second = second;
    }

    static [Symbol.customMatcher](pair) {
        return [pair.first, pair.second];
    }
}

class Triple {
    constructor(a, b, c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    static [Symbol.customMatcher](triple) {
        return [triple.a, triple.b, triple.c];
    }
}

const PlainObjectWithCustomMatcher = {
    [Symbol.customMatcher](obj) {
        return Object.entries(obj).sort();
    }
}
