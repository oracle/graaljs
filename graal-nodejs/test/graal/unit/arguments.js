/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

describe('Arguments', function () {
    describe('IsConstructCall', function () {
        it('should return false for a regular call', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            func(obj);
            assert.strictEqual(obj.isConstructCall, false);
        });
        it('should return true for a construct call', function () {
            var Func = module.Arguments_FunctionWithArguments();
            var obj = {};
            new Func(obj);
            assert.strictEqual(obj.isConstructCall, true);
        });
    });
    describe('This', function () {
        it('should return correct this value', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            var expectedThis = {a: 123};
            func.call(expectedThis, obj);
            var actualThis = obj.thisValue;
            assert.strictEqual(actualThis, expectedThis);
        });
    });
    describe('Holder', function () {
        //holder is usually identical to This, except for some corner cases e.g., around prototypes
        //https://groups.google.com/forum/#!topic/v8-users/Axf4hF_RfZo
        it('should return correct holder value', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            var expectedThis = {a: 123, b: "test"};
            func.call(expectedThis, obj);
            var actualHolder = obj.holderValue;
            assert.strictEqual(actualHolder, expectedThis);
        });
    });
    describe('arg[0]', function () {
        it('should be returned as it is from the identity function', function() {
            var lazyString = 'aaaaaaaaaaaaaaaaaaaa';
            lazyString += 'bbbbbbbbbbbbbbbbbbbbbbb';
            var values = [
                true,
                false,
                0,
                Infinity,
                -Infinity,
                Math.PI,
                'string',
                lazyString,
                Symbol.toStringTag,
                { foo: 'bar'},
                [1,2,3]
            ];
            if (module.hasJavaInterop()) {
                values.push(new (Java.type('java.awt.Point'))(42, 211));
                values.push(new (Java.type('java.math.BigDecimal'))(3.14));
            }
            values.forEach(function(value) {
                assert.strictEqual(module.Arguments_Identity(value), value);
            });
        });
    });
});
