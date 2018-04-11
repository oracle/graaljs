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

describe('Function', function () {
    describe('NewInstance', function () {
        it('without arguments', function () {
            var source = Object;
            var func = module.Function_NewInstance(source);
            assert.strictEqual(func instanceof source, true);
            assert.strictEqual(func === source, false);
        });
        it('with arguments', function () {
            var source = Object;
            var func = module.Function_NewInstance(source, 7, [0, 1, 2, 3, 4, 5, 6]);
            assert.strictEqual(func instanceof source, true);
            assert.strictEqual(func === source, false);
            //TODO check the set arguments
        });
    });
    describe('SetName', function () {
        it('simple name', function () {
            function SetNameTestFunc() {
                return 42;
            }
            ;
            var source = SetNameTestFunc;
            var newName = "NewNameTestFunc";
            var func = module.Function_SetName(source, newName);
            assert.strictEqual(func === source, true);
            assert.strictEqual(func.name === newName, true);
            assert.strictEqual(SetNameTestFunc.name === newName, true);
        });
    });
    describe('Call', function () {
        it('should call a simple function', function () {
            function SimpleFunction(a, b, c) {
                return a + b + c;
            }
            var rec = {};
            var result = module.Function_Call(SimpleFunction, rec, 3, 10, 11, 12);
            assert.strictEqual(typeof result, "number");
            assert.strictEqual(result, 33);
        });
        it('should call a simple function via proxy', function () {
            function identity(a) {
                return a;
            }
            var proxy = new Proxy(identity, { apply: function(f,thiz,args) {return  6 * f.apply(thiz, args)}});
            var target = {};
            var result = module.Function_Call(proxy, target, 1, 7);
            assert.strictEqual(typeof result, "number");
            assert.strictEqual(result, 42);
        });
    });
});
