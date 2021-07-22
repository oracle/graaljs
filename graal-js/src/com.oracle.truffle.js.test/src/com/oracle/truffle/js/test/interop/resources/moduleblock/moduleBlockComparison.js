/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

(async function() {
    let moduleBlock = module {
        export let y = 1;
    };

    let moduleExports = await import(moduleBlock);
    console.assert(moduleExports.y === 1, "Module block import/export failed.");

    console.assert(await import(moduleBlock) === moduleExports, "Not equal.");
})();

(async function() {
    const arr = new Array(2);

    for (let i = 0; i < 2; i++) {
        arr[i] = module {};
    }

    console.assert(arr[0] !== arr[1], "Different module blocks are the same.");
    console.assert(await import(arr[0]) !== await import(arr[1]), "Different imported module blocks are the same.");
})();

(async function() {
    const m1 = module {};
    const m2 = m1;

    console.assert(await import(m1) === await import(m2), "The same module block imported twice is not the same.");
})();

var moduleTest = (async function() {
    const moduleBlock = module { };

    return await import(moduleBlock);
})();

[moduleTest.then(v=>121), moduleTest.then(v=>5), moduleTest.then(v=>11)];

// This test can be conducted as soon as the realms proposal: https://github.com/tc39/proposal-realms is implemented
/*

(async function() {
    let moduleBlock = module {
        export let o = Object;
    };

    let m = await import(moduleBlock);
    console.assert(m.o === Object, "O export is not an object.");

    let r1 = new Realm();
    let m1 = await r1.import(moduleBlock);
    console.assert(m1.o === r1.globalThis.Object, "Realm o export is not an object.");
    console.assert(m1.o !== Object, "Realm o export is old realm object.");
    console.assert(m.o !== m1.o, "Both o's are equal.");
})();

*/
