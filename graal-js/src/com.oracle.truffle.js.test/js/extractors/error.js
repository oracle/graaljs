/**
 * @option js.extractors
 */

load('../assert.js');

{
    class C {
        #data;

        constructor(data) {
            this.#data = data;
        }

        static [Symbol.customMatcher](subject) {
            return undefined;
        }
    }

    const subject = new C("data");

    try {
        const C(x) = subject;
        assertTrue(false);
    } catch (e) {
        // ensure a meaningful error message is thrown
        assertTrue(e.message.includes("customMatcher"));
    }
}
