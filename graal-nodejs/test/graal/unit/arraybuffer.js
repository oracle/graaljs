/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

describe('ArrayBuffer', function () {
    var typedArrays = [
        'Uint8Array',
        'Uint8ClampedArray',
        'Int8Array',
        'Uint16Array',
        'Int16Array',
        'Uint32Array',
        'Int32Array',
        'Float32Array',
        'Float64Array',
        'BigInt64Array',
        'BigUint64Array'
    ];
    describe('Detach', function () {
        it('should set byteLength to 0', function () {
            var buffer = new ArrayBuffer(10);
            module.ArrayBuffer_Externalize(buffer);
            module.ArrayBuffer_Detach(buffer);
            assert.strictEqual(buffer.byteLength, 0);
        });
        it('should set content to null', function () {
            var buffer = new ArrayBuffer(10);
            module.ArrayBuffer_Externalize(buffer);
            module.ArrayBuffer_Detach(buffer);
            assert.strictEqual(module.ArrayBuffer_GetContentsDataPointerIsNull(buffer), true);
        });
        typedArrays.forEach(function (type) {
            it(type + '::New() can be used on a detached buffer', function () {
                var buffer = new ArrayBuffer(10);
                module.ArrayBuffer_Detach(buffer);
                var array = module['ArrayBuffer_New' + type](buffer);
                assert.strictEqual(array.byteLength, 0);
            });
        });
    });
    describe('GetContents', function () {
        it('should work on a regular buffer', function() {
            var buffer = new ArrayBuffer(6);
            var array = new Uint8Array(buffer);
            for (var i = 0; i < 6; i++) {
                array[i] = 2 * (i + 1);
            }
            var sum = module.ArrayBuffer_GetContentsSum(buffer);
            assert.strictEqual(sum, 42);
        });
        if (typeof java !== 'undefined') {
            it('should work on an interop buffer', function() {
                var buffer = java.nio.ByteBuffer.allocate(6);
                for (var i = 1; i <= 6; i++) {
                    buffer.put(2 * i);
                }
                var sum = module.ArrayBuffer_GetContentsSum(new ArrayBuffer(buffer));
                assert.strictEqual(sum, 42);
            });
        }
    });
    typedArrays.forEach(function (type) {
        it(type + '::New() can be used on a regular buffer', function () {
            var buffer = new ArrayBuffer(8);
            var array = module['ArrayBuffer_New' + type](buffer);
            assert.ok(array instanceof global[type]);
        });
        if (typeof java !== 'undefined') {
            it(type + '::New() can be used on an interop buffer', function () {
                var buffer = new ArrayBuffer(java.nio.ByteBuffer.allocate(8));
                var array = module['ArrayBuffer_New' + type](buffer);
                assert.ok(array instanceof global[type]);
            });
        }
    });
});
