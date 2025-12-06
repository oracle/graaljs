/**
 * @option js.extractors
 */

/**
 * Receiver Parameter in CustomMatcher
 *
 * Tests that the customMatcher receives the receiver (extractor expression)
 * as the third parameter when invoked.
 */

load('../assert.js');

// Receiver Parameter - Basic Access
{
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
}

// Receiver Parameter - Instance Method Access
{
    class Processor {
        #transform;

        constructor(transform) {
            this.#transform = transform;
        }

        get matcher() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [receiver.#transform(subject)];
                }
            };
        }
    }

    const processor = new Processor(x => x * 2);
    const processor.matcher(result) = 5;

    assertSame(10, result);
}

// Receiver Parameter - Accessing Receiver Properties
{
    class Wrapper {
        constructor(prefix) {
            this.prefix = prefix;
        }

        extractor = {
            [Symbol.customMatcher](subject, _kind, receiver) {
                return [receiver.prefix + subject];
            }
        };
    }

    const wrapper = new Wrapper("PREFIX:");
    const wrapper.extractor(value) = "test";

    assertSame("PREFIX:test", value);
}

// Receiver Parameter - Multiple Receivers
{
    class Factory {
        constructor(multiplier) {
            this.multiplier = multiplier;
        }

        get extractor() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [subject * receiver.multiplier];
                }
            };
        }
    }

    const factory1 = new Factory(10);
    const factory2 = new Factory(100);

    const factory1.extractor(x) = 5;
    const factory2.extractor(y) = 5;

    assertSame(50, x);
    assertSame(500, y);
}

// Receiver Parameter - Static vs Instance Context
{
    class Extractor {
        static staticValue = "static";
        instanceValue = "instance";

        static staticExtractor = {
            [Symbol.customMatcher](subject, _kind, receiver) {
                // When accessed as Extractor.staticExtractor, receiver is the class
                return [receiver.staticValue];
            }
        };

        get instanceExtractor() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    // When accessed as obj.instanceExtractor, receiver is the instance
                    return [receiver.instanceValue];
                }
            };
        }
    }

    const Extractor.staticExtractor(staticResult) = "ignored";
    assertSame("static", staticResult);

    const obj = new Extractor();
    const obj.instanceExtractor(instanceResult) = "ignored";
    assertSame("instance", instanceResult);
}

// Receiver Parameter - Computed Property Access
{
    class Container {
        #value;

        constructor(value) {
            this.#value = value;
        }

        get processor() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [receiver.#value + subject];
                }
            };
        }
    }

    const containers = [new Container(10), new Container(100)];

    const containers[0].processor(x) = 5;
    const containers[1].processor(y) = 5;

    assertSame(15, x);
    assertSame(105, y);
}

// Receiver Parameter - Method Call on Receiver
{
    class Transformer {
        transform(value) {
            return `transformed:${value}`;
        }

        get extractor() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [receiver.transform(subject)];
                }
            };
        }
    }

    const t = new Transformer();
    const t.extractor(result) = "input";

    assertSame("transformed:input", result);
}

// Receiver Parameter - Arrow Function Access
{
    class Closer {
        constructor(closure) {
            this.closure = closure;
            this.extractor = {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [receiver.closure(subject)];
                }
            };
        }
    }

    const closer = new Closer(x => x.split('').reverse().join(''));
    const closer.extractor(reversed) = "hello";

    assertSame("olleh", reversed);
}

// Receiver Parameter - Nested Property Access
{
    class Config {
        constructor() {
            this.settings = {
                multiplier: 3,
                offset: 7
            };
        }

        get calculator() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [subject * receiver.settings.multiplier + receiver.settings.offset];
                }
            };
        }
    }

    const config = new Config();
    const config.calculator(result) = 10;

    assertSame(37, result); // 10 * 3 + 7
}

// Receiver Parameter - this Context
{
    const obj = {
        value: 42,
        extractor: {
            [Symbol.customMatcher](subject, _kind, receiver) {
                // receiver should be obj when accessed as obj.extractor
                return [receiver.value];
            }
        }
    };

    const obj.extractor(x) = "anything";
    assertSame(42, x);
}

// Receiver Parameter - Symbol Properties
{
    const sym = Symbol('data');

    class SymbolHolder {
        constructor(data) {
            this[sym] = data;
        }

        get extractor() {
            return {
                [Symbol.customMatcher]: (subject, _kind, receiver) => {
                    return [receiver[sym]];
                }
            };
        }
    }

    const holder = new SymbolHolder("symbol-data");
    const holder.extractor(data) = "ignored";

    assertSame("symbol-data", data);
}

