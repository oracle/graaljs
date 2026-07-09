/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests AsyncContext proposal.
 *
 * @option unhandled-rejections=throw
 * @option async-context
 * @option testV8-mode
 */

load("assert.js");

const context = new AsyncContext.Variable({name: 'ctx', defaultValue: 'default'});
const timeout = 10; // random timeout

if (typeof setTimeout === 'undefined' && typeof TestV8 !== 'undefined') {
  setTimeout = TestV8.setTimeout;
}

assertSame(context.name, 'ctx');
assertSame(context.get(), 'default');

context.run("top", main);

function main() {
  setTimeout(() => {
    assertSame(context.get(), 'top');

    context.run("A", () => {
      assertSame(context.get(), 'A');

      setTimeout(() => {
        assertSame(context.get(), 'A');
      }, timeout);
    });
  }, timeout);

  context.run("B", () => {
    assertSame(context.get(), 'B');

    setTimeout(() => {
      assertSame(context.get(), 'B');
    }, timeout);
  });

  assertSame(context.get(), 'top');

  const snapshotDuringTop = new AsyncContext.Snapshot();

  context.run("C", () => {
    assertSame(context.get(), 'C');

    snapshotDuringTop.run(() => {
      assertSame(context.get(), 'top');
    });
  });
}

(function testNames() {
    assertSame(AsyncContext[Symbol.toStringTag], 'AsyncContext');
    assertSame(String(AsyncContext), '[object AsyncContext]');
    assertSame(AsyncContext.Variable.prototype[Symbol.toStringTag], 'AsyncContext.Variable');
    assertSame(AsyncContext.Snapshot.prototype[Symbol.toStringTag], 'AsyncContext.Snapshot');
    assertSame(String(new AsyncContext.Variable()), '[object AsyncContext.Variable]');
    assertSame(String(new AsyncContext.Snapshot()), '[object AsyncContext.Snapshot]');
    assertSame(AsyncContext.Variable.name, 'Variable');
    assertSame(AsyncContext.Snapshot.name, 'Snapshot');
    
    assertSame(new AsyncContext.Variable().name, '');
    assertSame(new AsyncContext.Variable({}).name, '');
})();

(function testSnapshotWrap() {
    const receiver = {};
    let wrapped;
    function target(a, b) {
        assertSame(this, receiver);
        return [context.get(), a, b];
    }
    context.run('wrapped', () => {
        wrapped = AsyncContext.Snapshot.wrap(target);
    });
    assertSame(wrapped.name, 'wrapped target');
    assertSame(wrapped.length, 2);
    assertSame(wrapped.call(receiver, 1, 2).join(','), 'wrapped,1,2');
    assertThrows(() => AsyncContext.Snapshot.wrap(42), TypeError);
})();

(function testSnapshotWrapCopyNameAndLength() {
    function target() {
    }
    let lengthGets = 0;
    let nameGets = 0;
    Object.defineProperties(target, {
        length: {
            configurable: true,
            get() {
                lengthGets++;
                return 2.9;
            }
        },
        name: {
            configurable: true,
            get() {
                nameGets++;
                return 'copied';
            }
        }
    });
    const wrapped = AsyncContext.Snapshot.wrap(target);
    assertSame(lengthGets, 1);
    assertSame(nameGets, 1);
    assertSame(wrapped.length, 2);
    assertSame(wrapped.name, 'wrapped copied');

    function targetWithoutLength() {
    }
    delete targetWithoutLength.length;
    delete targetWithoutLength.name;
    Object.setPrototypeOf(targetWithoutLength, {length: 12, name: 'inherited'});
    const wrappedWithoutLength = AsyncContext.Snapshot.wrap(targetWithoutLength);
    assertSame(wrappedWithoutLength.length, 0);
    assertSame(wrappedWithoutLength.name, 'wrapped inherited');
})();

(function testGeneratorContext() {
    let generator;
    context.run('generator', () => {
        generator = (function* () {
            assertSame(context.get(), 'generator');
            yield 1;
            assertSame(context.get(), 'generator');
            yield 2;
        })();
    });
    context.run('generator caller 1', () => {
        assertSame(generator.next().value, 1);
        assertSame(context.get(), 'generator caller 1');
    });
    context.run('generator caller 2', () => {
        assertSame(generator.next().value, 2);
        assertSame(context.get(), 'generator caller 2');
    });
})();

(function testAsyncGeneratorContext() {
    let generator;
    context.run('async generator', () => {
        generator = (async function* () {
            assertSame(context.get(), 'async generator');
            yield 1;
            await Promise.resolve();
            assertSame(context.get(), 'async generator');
            yield 2;
        })();
    });
    let first;
    context.run('async generator caller 1', () => {
        first = generator.next();
        first.then((result) => {
            assertSame(result.value, 1);
            assertSame(context.get(), 'async generator caller 1');
            let second;
            context.run('async generator caller 2', () => {
                second = generator.next();
                assertSame(context.get(), 'async generator caller 2');
            });
            return second;
        }).then((result) => {
            assertSame(result.value, 2);
            assertSame(context.get(), 'async generator caller 1');
        });
        assertSame(context.get(), 'async generator caller 1');
    });
})();

(function testPromiseReactionContext() {
    const thenable = {
        then(resolve) {
            assertSame(context.get(), 'promise');
            resolve();
        }
    };
    context.run('promise', () => {
        Promise.resolve().then(() => {
            assertSame(context.get(), 'promise');
            return thenable;
        }).then(() => {
            assertSame(context.get(), 'promise');
        });
    });
})();

(function testPromiseReactionRejectionContext() {
    const reason = new Error('expected rejection');
    const thenable = {
        then(resolve, reject) {
            assertSame(context.get(), 'rejected');
            reject(reason);
        }
    };
    context.run('rejected', () => {
        Promise.resolve().then(() => {
            assertSame(context.get(), 'rejected');
            return thenable;
        }).then(undefined, (rejection) => {
            assertSame(rejection, reason);
            assertSame(context.get(), 'rejected');
        });
    });
})();
