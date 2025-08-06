load('../assert.js');

class C {
    #data;
    constructor(data) {
        this.#data = data;
    }
    static [Symbol.customMatcher](subject) {
        return #data in subject && [subject.#data];
    }
}

const subject = new C("data");

{
    let C(x) = subject;
    assertSame(x, "data");
}

{
    const C(x) = subject;
    assertSame(x, "data");
}