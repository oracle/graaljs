load('../js/assert.js');

class C {
    #data1;
    constructor(data1) {
        this.#data1 = data1;
    }
    static [Symbol.customMatcher](subject) {
        return #data1 in subject && [subject.#data1];
    }
}

class D {
    #data2;
    constructor(data2) {
        this.#data2 = data2;
    }
    static [Symbol.customMatcher](subject) {
        return #data2 in subject && [subject.#data2];
    }
}

const subject = new C(new D("data"));

const C(D(x)) = subject;

assertSame(x, "data");

const { a: C(D(y)) } = { a: subject };
assertSame(y, "data");

const { a: { b: C(D(z)) } } = { a: { b: subject } };
assertSame(z, "data");``
