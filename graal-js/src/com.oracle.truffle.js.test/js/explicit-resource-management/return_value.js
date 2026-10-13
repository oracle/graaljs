/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * A `using` declaration must not drop the value of the block that ends its scope.
 *
 * @option explicit-resource-management
 * @option unhandled-rejections=throw
 */

load('../assert.js');

const disposed = [];
const resource = (name) => ({ [Symbol.dispose]() { disposed.push(name); } });

function body() { using x = resource('body'); return 'body'; }
function nested() { { using x = resource('nested'); return 'nested'; } }
function early(flag) { using x = resource('early'); if (flag) return 'early'; const y = 1; return y + 1; }
function none() { using x = resource('none'); }
const arrow = () => { using x = resource('arrow'); return 'arrow'; };
function* gen() { using x = resource('gen'); yield 1; return 'gen'; }
function loop() { for (using x of [resource('loop'), resource('loop')]) { } return 'loop'; }

assertSame('body', body());
assertSame('nested', nested());
assertSame('early', early(true));
assertSame(2, early(false));
assertSame(undefined, none());
assertSame('arrow', arrow());
const it = gen();
assertSame(1, it.next().value);
assertSame('gen', it.next().value);
assertSame('loop', loop());
assertThrows(() => { using x = resource('throw'); throw new TypeError('boom'); }, TypeError);
assertSame('body,nested,early,early,none,arrow,gen,loop,loop,throw', disposed.join(','));

async function awaitUsing() {
    await using x = { async [Symbol.asyncDispose]() { await null; disposed.push('async'); } };
    return 'async';
}
awaitUsing().then((value) => {
    assertSame('async', value);
    assertSame('async', disposed[disposed.length - 1]);
});
