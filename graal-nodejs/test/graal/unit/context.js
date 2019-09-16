/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

var assert = require('assert');
var module = require('./_unit');

describe('Context', function () {
    describe('GetGlobal', function () {
        it('get global object', function () {
            var global = module.Context_Global();

            assert.strictEqual(global instanceof Object, true);
            assert.strictEqual(Object.keys(global).length > 20, true);
            assert.strictEqual(global.hasOwnProperty("Array"), true);
            assert.strictEqual(global["Array"] === Array, true);
        });
    });
    describe('Enter / Exit', function () {
        it('enter and exit a new Context', function () {
            assert.strictEqual(module.Context_EnterAndExitNewContext(), true);
        });
    });
    describe('New', function () {
        it('can create a global object with an additional property ', function () {
            assert.strictEqual(module.Context_New(), true);
        });
    });
    describe('SecurityTokens', function () {
        it('get security token', function () {
            var token = module.Context_GetSecurityToken();
            assert.strictEqual(token instanceof Object, true);
        });
        it('set the same security token again', function () {
            var token1 = module.Context_GetSecurityToken();
            assert.strictEqual(token1 instanceof Object, true);
            module.Context_SetSecurityToken(token1);
            var token2 = module.Context_GetSecurityToken();
            assert.strictEqual(token1 === token2, true);
        });
        it('call UseDefaultSecurityToken', function () {
            assert.strictEqual(module.Context_UseDefaultSecurityToken(), true);
        });
    });
    it.skipOnNode('should store Environment at index 32', function () {
        // If this test fails then GraalContext::kNodeContextEmbedderDataIndex should be updated
        assert.strictEqual(module.Context_IndexOfEnvironment(), 32);
    });
});
