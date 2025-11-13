load('../js/assert.js');

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

    {
        let x;
        C(x) = subject;
        assertSame("data", x);
    }

    {
        let C(x) = subject;
        assertSame("data", x);
    }

    {
        var C(x) = subject;
        assertSame("data", x);
    }

    {
        const C(x) = subject;
        assertSame("data", x);
    }
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

    {
        let x, y;
        C({x, y}) = subject;

        assertSame(1, x);
        assertSame(2, y);
    }

    {
        let C({x, y}) = subject;

        assertSame(1, x);
        assertSame(2, y);
    }
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

    {
        let x = -1, y = 100;
        C(x = -1, y) = subject;
        assertSame(-1, x);
        assertSame(2, y);
    }

    {
        const C(x = -1, y) = subject;
        assertSame(-1, x);
        assertSame(2, y);
    }
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

    {
        let x, y;
        C(x, ...y) = subject;
        assertSame(1, x);
        assertSameContent([2, 3], y);
    }

    {
        const C(x, ...y) = subject;
        assertSame(1, x);
        assertSameContent([2, 3], y);
    }

    {
        assertSame(subject, C() = subject)
        assertSame(subject, C(x) = subject)
        assertSame(subject, C(x, ...y) = subject)
    }
}
