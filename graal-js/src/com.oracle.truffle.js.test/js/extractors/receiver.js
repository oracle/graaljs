load('../assert.js');

class C {
    #f;
    constructor(f) {
        this.#f = f;
    }
    extractor = {
        [Symbol.customMatcher](subject, _kind, receiver) {
            return [receiver.#f(subject)];
        }
    };
}

const obj = new C(data => data.toUpperCase() + "1234");
const subject = "data";

const obj.extractor(x) = subject;

assertSame(x, "DATA1234");
