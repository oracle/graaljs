/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

const assert = require('node:assert');
globalThis.importedUrl = './dynamically_imported.mjs';
globalThis.nonExistent = './does-not-exist.mjs';

describe('dynamic import', function () {

    describe('of existing module should work', function () {
        it('from normal import expression', async function () {
            const module = await import(importedUrl);
            assert.strictEqual(42, module.default);
        });
        it('from direct eval', async function () {
            const module = await eval("import(importedUrl)");
            assert.strictEqual(42, module.default);
        });
        it('from indirect eval', async function () {
            const module = await (0,eval)("import(importedUrl)");
            assert.strictEqual(42, module.default);
        });
        it('from indirect eval via call method', async function () {
            const module = await eval.call(undefined, "import(importedUrl)");
            assert.strictEqual(42, module.default);
        });
        it('from dynamic function', async function () {
            const module = await (new Function("return import(importedUrl)"))();
            assert.strictEqual(42, module.default);
        });
        it('from dynamic function via Reflect.construct', async function () {
            const module = await Reflect.construct(Function, ["return import(importedUrl)"])();
            assert.strictEqual(42, module.default);
        });
    });

    describe('of non-existent module should throw ERR_MODULE_NOT_FOUND', function () {
        const ERR_MODULE_NOT_FOUND = err => {
            assert(err instanceof Error);
            assert.strictEqual(err.name, 'Error');
            assert.strictEqual(err.code, 'ERR_MODULE_NOT_FOUND');
            return true;
        };
        it('from normal import expression', async function () {
            await assert.rejects(
                async () => await import(nonExistent),
                ERR_MODULE_NOT_FOUND
            );
        });
        it('from direct eval', async function () {
            await assert.rejects(
                async () => await eval("import(nonExistent)"),
                ERR_MODULE_NOT_FOUND
            );
        });
        it('from indirect eval', async function () {
            await assert.rejects(
                async () => await (0,eval)("import(nonExistent)"),
                ERR_MODULE_NOT_FOUND
            );
        });
        it('from indirect eval via call method', async function () {
            await assert.rejects(
                async () => await eval.call(undefined, "import(nonExistent)"),
                ERR_MODULE_NOT_FOUND
            );
        });
        it('from dynamic function', async function () {
            await assert.rejects(
                async () => await (new Function("return import(nonExistent)"))(),
                ERR_MODULE_NOT_FOUND
            );
        });
        it('from dynamic function via Reflect.construct', async function () {
            await assert.rejects(
                async () => await Reflect.construct(Function, ["return import(nonExistent)"])(),
                ERR_MODULE_NOT_FOUND
            );
        });
    });

    it('should import from active script or module', async function () {
        function importFromScriptFunction(src) {
            return new Function(`return import(${JSON.stringify(src)})`);
        }
        const module = await import(importedUrl);
        try {
            await (module.importFromModuleFunction(nonExistent)());
            assert.fail("should have thrown");
        } catch (e) {
            assert(e.message.includes('from') && e.message.includes('imported.mjs'), e.message);
        }

        try {
            await (module.callAsync(importFromScriptFunction(), nonExistent));
            assert.fail("should have thrown");
        } catch (e) {
            assert(e.message.includes('from') && !e.message.includes('imported.mjs'), e.message);
        }

        try {
            await (module.indirectEval("import(nonExistent)"));
            assert.fail("should have thrown");
        } catch (e) {
            assert(e.message.includes('from') && e.message.includes('imported.mjs'), e.message);
        }

        try {
            await (module.directEval("import(nonExistent)"));
            assert.fail("should have thrown");
        } catch (e) {
            assert(e.message.includes('from') && e.message.includes('imported.mjs'), e.message);
        }
    });

});
