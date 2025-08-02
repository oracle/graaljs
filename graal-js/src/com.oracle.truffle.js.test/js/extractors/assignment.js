load('../assert.js');

{
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

    let x;
    C(x) = subject;

    assertSame(x, "data");
}

{
    class C {
        #data;
        constructor(data) {
            this.#data = data;
        }
        static [Symbol.customMatcher](subject) {
            return #data in subject && [subject.#data];
        }
    }

    const subject = new C({ x: 1, y: 2 });

    let x, y;
    C({ x, y }) = subject;

    assertSame(x, 1);
    assertSame(y, 2);
}

{
    class C {
        #first;
        #second;
        constructor(first, second) {
            this.#first = first;
            this.#second = second;
        }
        static [Symbol.customMatcher](subject) {
            return #first in subject && [subject.#first, subject.#second];
        }
    }

    const subject = new C(undefined, 2);

    const C(x = -1, y) = subject;
    assertSame(x, -1);
    assertSame(y, 2);
}

{
    const [a, ...b] = [1, 2, 3];

    class C {
        #first;
        #second;
        #third;
        constructor(first, second, third) {
            this.#first = first;
            this.#second = second;
            this.#third = third;
        }
        static [Symbol.customMatcher](subject) {
            return #first in subject && [subject.#first, subject.#second, subject.#third];
        }
    }

    const subject = new C(1, 2, 3);

    const C(x, ...y) = subject;
    assertSame(x, 1);
    assertSameContent(y, [2, 3]);
}
