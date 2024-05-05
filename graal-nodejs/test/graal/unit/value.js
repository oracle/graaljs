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

// Many Value tests are the same as other tests with just casting added.
// They are here mostly to ensure that the corresponding methods are virtual.
describe('Value - Is*()', function () {
    describe('IsUndefined', function () {
        it('should return true for Undefined casted to Value', function () {
            assert.strictEqual(module.Value_IsUndefinedForUndefined(), true);
        });
        it('should return false for Null casted to Value', function () {
            assert.strictEqual(module.Value_IsUndefinedForNull(), false);
        });
    });
    describe('IsNull', function () {
        it('should return false for Undefined casted to Value', function () {
            assert.strictEqual(module.Value_IsNullForUndefined(), false);
        });
        it('should return true for Null casted to Value', function () {
            assert.strictEqual(module.Value_IsNullForNull(), true);
        });
    });
    describe('IsTrue', function () {
        it('should return true for True casted to Value', function () {
            assert.strictEqual(module.Value_IsTrueForTrue(), true);
        });
        it('should return false for False casted to Value', function () {
            assert.strictEqual(module.Value_IsTrueForFalse(), false);
        });
    });
    describe('IsFalse', function () {
        it('should return false for True casted to Value', function () {
            assert.strictEqual(module.Value_IsFalseForTrue(), false);
        });
        it('should return true for False casted to Value', function () {
            assert.strictEqual(module.Value_IsFalseForFalse(), true);
        });
    });
    describe('IsBoolean', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsBoolean(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsBoolean(null), false);
        });
        it('should return true for true', function () {
            assert.strictEqual(module.Value_IsBoolean(true), true);
        });
        it('should return true for false', function () {
            assert.strictEqual(module.Value_IsBoolean(false), true);
        });
        it('should return false for 0', function () {
            assert.strictEqual(module.Value_IsBoolean(0), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsBoolean({}), false);
        });
        it('should return false for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsBoolean([1, 2, 3]), false);
        });
        it('should return false for Array(10)', function () {
            assert.strictEqual(module.Value_IsBoolean(Array(10)), false);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsBoolean('.*'), false);
        });
        it('should return false for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsBoolean(/a+b*c/), false);
        });
        it('should return false for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsBoolean(new RegExp("a+b*c")), false);
        });
    });
    describe('IsNumber', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsNumber(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsNumber(null), false);
        });
        it('should return true for 0', function () {
            assert.strictEqual(module.Value_IsNumber(0), true);
        });
        it('should return true for -0.0', function () {
            assert.strictEqual(module.Value_IsNumber(-0.0), true);
        });
        it('should return true for Math.pow(2,52)', function () {
            assert.strictEqual(module.Value_IsNumber(Math.pow(2, 52)), true);
        });
        it('should return false for "true"', function () {
            assert.strictEqual(module.Value_IsNumber(true), false);
        });
        it('should return false for "false"', function () {
            assert.strictEqual(module.Value_IsNumber(false), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsNumber({}), false);
        });
        it('should return false for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsNumber([1, 2, 3]), false);
        });
        it('should return false for Array(10)', function () {
            assert.strictEqual(module.Value_IsNumber(Array(10)), false);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsNumber('.*'), false);
        });
        it('should return false for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsNumber(/a+b*c/), false);
        });
        it('should return false for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsNumber(new RegExp("a+b*c")), false);
        });
    });
    describe('IsInt32', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsInt32(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsInt32(null), false);
        });
        it('should return true for 0', function () {
            assert.strictEqual(module.Value_IsInt32(0), true);
        });
        it('should return false for -0', function () {
            assert.strictEqual(module.Value_IsInt32(-0), false);
        });
        it('should return false for 0.5', function () {
            assert.strictEqual(module.Value_IsInt32(0.5), false);
        });
        it('should return true for Math.pow(2,31)-1', function () {
            assert.strictEqual(module.Value_IsInt32(Math.pow(2, 31) - 1), true);
        });
        it('should return false for Math.pow(2,31)', function () {
            assert.strictEqual(module.Value_IsInt32(Math.pow(2, 31)), false);
        });
        it('should return true for -Math.pow(2,31)', function () {
            assert.strictEqual(module.Value_IsInt32(-Math.pow(2, 31)), true);
        });
        it('should return false for -Math.pow(2,31)-1', function () {
            assert.strictEqual(module.Value_IsInt32(-Math.pow(2, 31) - 1), false);
        });
    });
    describe('IsUint32', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsUint32(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsUint32(null), false);
        });
        it('should return true for 0', function () {
            assert.strictEqual(module.Value_IsUint32(0), true);
        });
        it('should return false for -0', function () {
            assert.strictEqual(module.Value_IsUint32(-0), false);
        });
        it('should return false for -5', function () {
            assert.strictEqual(module.Value_IsUint32(-5), false);
        });
        it('should return true for Math.pow(2,32)-1', function () {
            assert.strictEqual(module.Value_IsUint32(Math.pow(2, 32) - 1), true);
        });
        it('should return false for Math.pow(2,32)', function () {
            assert.strictEqual(module.Value_IsUint32(Math.pow(2, 32)), false);
        });
    });
    describe('IsObject', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsObject(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsObject(null), false);
        });
        it('should return false for "true"', function () {
            assert.strictEqual(module.Value_IsObject(true), false);
        });
        it('should return false for "false"', function () {
            assert.strictEqual(module.Value_IsObject(false), false);
        });
        it('should return true for {}', function () {
            assert.strictEqual(module.Value_IsObject({}), true);
        });
        it('should return true for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsObject([1, 2, 3]), true);
        });
        it('should return true for Array(10)', function () {
            assert.strictEqual(module.Value_IsObject(Array(10)), true);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsObject('.*'), false);
        });
        it('should return true for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsObject(/a+b*c/), true);
        });
        it('should return true for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsObject(new RegExp("a+b*c")), true);
        });
    });
    describe('IsArray', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsArray(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsArray(null), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsArray({}), false);
        });
        it('should return true for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsArray([1, 2, 3]), true);
        });
        it('should return true for Array(10)', function () {
            assert.strictEqual(module.Value_IsArray(Array(10)), true);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsArray('.*'), false);
        });
        it('should return false for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsArray(/a+b*c/), false);
        });
        it('should return false for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsArray(new RegExp("a+b*c")), false);
        });
    });
    describe('IsFunction', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsFunction(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsFunction(null), false);
        });
        it('should return true for anonymous function', function () {
            assert.strictEqual(module.Value_IsFunction(function () {
                return undefined;
            }), true);
        });
        it('should return true for function proxy', function () {
            assert.strictEqual(module.Value_IsFunction(new Proxy(function () {
                    return undefined;
                }, {})
            ), true);
        });
        it('should return false for non-function proxy', function () {
            assert.strictEqual(module.Value_IsFunction(new Proxy({}, {})), false);
        });
        it('should return true for Object.prototype.toString', function () {
            assert.strictEqual(module.Value_IsFunction(Object.prototype.toString), true);
        });
        it('should return false for "true"', function () {
            assert.strictEqual(module.Value_IsFunction(true), false);
        });
        it('should return false for "false"', function () {
            assert.strictEqual(module.Value_IsFunction(false), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsFunction({}), false);
        });
        it('should return false for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsFunction([1, 2, 3]), false);
        });
        it('should return false for Array(10)', function () {
            assert.strictEqual(module.Value_IsFunction(Array(10)), false);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsFunction('.*'), false);
        });
        it('should return false for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsFunction(/a+b*c/), false);
        });
        it('should return false for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsFunction(new RegExp("a+b*c")), false);
        });
    });
    describe('IsRegExp', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsRegExp(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_IsRegExp(null), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsRegExp({}), false);
        });
        it('should return false for [1,2,3]', function () {
            assert.strictEqual(module.Value_IsRegExp([1, 2, 3]), false);
        });
        it('should return false for Array(10)', function () {
            assert.strictEqual(module.Value_IsRegExp(Array(10)), false);
        });
        it('should return false for ".*"', function () {
            assert.strictEqual(module.Value_IsRegExp('.*'), false);
        });
        it('should return true for /a+b*c/', function () {
            assert.strictEqual(module.Value_IsRegExp(/a+b*c/), true);
        });
        it('should return true for new RegExp("a+b*c")', function () {
            assert.strictEqual(module.Value_IsRegExp(new RegExp("a+b*c")), true);
        });
    });
    describe('IsNativeError', function () {
        it('should return true for RangeError', function () {
            assert.strictEqual(module.Value_IsNativeErrorForRangeError(), true);
        });
        it('should return true for ReferenceError', function () {
            assert.strictEqual(module.Value_IsNativeErrorForReferenceError(), true);
        });
        it('should return true for SyntaxError', function () {
            assert.strictEqual(module.Value_IsNativeErrorForSyntaxError(), true);
        });
        it('should return true for TypeError', function () {
            assert.strictEqual(module.Value_IsNativeErrorForTypeError(), true);
        });
        it('should return true for Error', function () {
            assert.strictEqual(module.Value_IsNativeErrorForError(), true);
        });
        it('should return false for String', function () {
            assert.strictEqual(module.Value_IsNativeError('error'), false);
        });
    });
    describe('IsExternal', function () {
        it('should return false for primitive data types', function () {
            assert.strictEqual(module.Value_IsExternal(""), false);
            assert.strictEqual(module.Value_IsExternal(4242), false);
            assert.strictEqual(module.Value_IsExternal(3.1415), false);
            assert.strictEqual(module.Value_IsExternal([0, 1, 2]), false);
            assert.strictEqual(module.Value_IsExternal({}), false);
            assert.strictEqual(module.Value_IsExternal(function () {
                return undefined;
            }), false);
        });
    });
    describe('IsMapIterator', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsMapIterator(undefined), false);
        });
        it('should return true for simple Map iterator', function () {
            var map = new Map([[1, "value1"], [2, "value2"], [3, "value3"]]);
            assert.strictEqual(module.Value_IsMapIterator(map.values()), true);
            assert.strictEqual(module.Value_IsMapIterator(map.entries()), true);
            assert.strictEqual(module.Value_IsMapIterator(map.keys()), true);
            assert.strictEqual(module.Value_IsMapIterator(map[Symbol.iterator]()), true);
        });
        it('should return false for simple Set iterator', function () {
            var set = new Set([1, 2, 3]);
            assert.strictEqual(module.Value_IsMapIterator(set.values()), false);
        });
    });
    describe('IsSetIterator', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsSetIterator(undefined), false);
        });
        it('should return true for simple Set iterator', function () {
            var set = new Set([1, 2, 3]);
            assert.strictEqual(module.Value_IsSetIterator(set.values()), true);
            assert.strictEqual(module.Value_IsSetIterator(set.entries()), true);
            assert.strictEqual(module.Value_IsSetIterator(set.keys()), true);
            assert.strictEqual(module.Value_IsSetIterator(set[Symbol.iterator]()), true);
        });
        it('should return false for simple Map iterator', function () {
            var map = new Map([[1, "value1"], [2, "value2"], [3, "value3"]]);
            assert.strictEqual(module.Value_IsSetIterator(map.values()), false);
        });
    });
    describe('IsDataView', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsDataView(undefined), false);
        });
        it('should return false for ArrayBuffer', function () {
            assert.strictEqual(module.Value_IsDataView(new ArrayBuffer(10)), false);
        });
        it('should return false for Uint8Array', function () {
            assert.strictEqual(module.Value_IsDataView(new Uint8Array(10)), false);
        });
        it('should return true for DataView', function () {
            assert.strictEqual(module.Value_IsDataView(new DataView(new ArrayBuffer(10))), true);
        });
    });
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
    typedArrays.forEach(function (type) {
        describe('Is' + type, function () {
            var testedFunction = module['Value_Is' + type];
            it('should return false for undefined', function () {
                assert.strictEqual(testedFunction(undefined), false);
            });
            it('should return false for ArrayBuffer', function () {
                assert.strictEqual(testedFunction(new ArrayBuffer(10)), false);
            });
            it('should return false for DataView', function () {
                assert.strictEqual(testedFunction(new DataView(new ArrayBuffer(10))), false);
            });
            typedArrays.forEach(function (valueType) {
                var expectedResult = valueType === type;
                var value = new global[valueType];
                it('should return ' + expectedResult + ' for ' + valueType, function () {
                    assert.strictEqual(testedFunction(value), expectedResult);
                });
                if (module.hasJavaInterop()) {
                    it('should return ' + expectedResult + ' for interop ' + valueType, function () {
                        var interopTypedArray = new (global[valueType])(java.nio.ByteBuffer.allocate(8));
                        assert.strictEqual(testedFunction(interopTypedArray), expectedResult);
                    });
                }
            });
        });
    });
    describe('IsPromise', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsPromise(undefined), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsPromise({}), false);
        });
        it('should return true for simple Promise', function () {
            assert.strictEqual(module.Value_IsPromise(new Promise(function () {})), true);
        });
    });
    describe('IsProxy', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsProxy(undefined), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(module.Value_IsProxy({}), false);
        });
        it('should return true for a simple Proxy', function () {
            assert.strictEqual(module.Value_IsProxy(new Proxy({}, {})), true);
        });
    });
    describe('IsMap', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsMap(undefined), false);
        });
        it('should return true for Map', function () {
            assert.strictEqual(module.Value_IsMap(new Map()), true);
        });
        it('should return false for Set', function () {
            assert.strictEqual(module.Value_IsMap(new Set()), false);
        });
    });
    describe('IsSet', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsSet(undefined), false);
        });
        it('should return false for Map', function () {
            assert.strictEqual(module.Value_IsSet(new Map()), false);
        });
        it('should return true for Set', function () {
            assert.strictEqual(module.Value_IsSet(new Set()), true);
        });
    });
    describe('IsString', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsString(undefined), false);
        });
        it('should return true for string', function () {
            assert.strictEqual(module.Value_IsString('foo'), true);
        });
        it('should return false for String object', function () {
            assert.strictEqual(module.Value_IsString(new String('foo')), false);
        });
        it('should return false for Symbol', function () {
            assert.strictEqual(module.Value_IsString(Symbol.toStringTag), false);
        });
    });
    describe('IsSymbol', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsSymbol(undefined), false);
        });
        it('should return false for string', function () {
            assert.strictEqual(module.Value_IsSymbol('foo'), false);
        });
        it('should return false for String object', function () {
            assert.strictEqual(module.Value_IsSymbol(new String('foo')), false);
        });
        it('should return true for Symbol', function () {
            assert.strictEqual(module.Value_IsSymbol(Symbol.toStringTag), true);
        });
    });
    describe('IsName', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsName(undefined), false);
        });
        it('should return true for string', function () {
            assert.strictEqual(module.Value_IsName('foo'), true);
        });
        it('should return false for String object', function () {
            assert.strictEqual(module.Value_IsName(new String('foo')), false);
        });
        it('should return true for Symbol', function () {
            assert.strictEqual(module.Value_IsName(Symbol.toStringTag), true);
        });
        it('should return false for 42', function () {
            assert.strictEqual(module.Value_IsName(42), false);
        });
    });
    describe('IsWasmMemoryObject', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_IsWasmMemoryObject(undefined), false);
        });
        it('should return false for an ordinary object', function () {
            assert.strictEqual(module.Value_IsWasmMemoryObject({}), false);
        });
        if (typeof WebAssembly !== 'undefined') {
            it('should return false for WebAssembly object', function () {
                assert.strictEqual(module.Value_IsWasmMemoryObject(WebAssembly), false);
            });
            it('should return true for WebAssembly.Memory object', function () {
                let memory = new WebAssembly.Memory({ initial: 1 });
                assert.strictEqual(module.Value_IsWasmMemoryObject(memory), true);
            });
            it('should return false for WebAssembly.Global object', function () {
                let wasmGlobal = new WebAssembly.Global({ value: 'i32' });
                assert.strictEqual(module.Value_IsWasmMemoryObject(wasmGlobal), false);
            });
        }
    });
});

describe('Value - *Value()', function () {
    // Value::IntegerValue() returns int64_t => module.Value_IntegerValue()
    // returns BigInt to avoid a loss of precision.
    describe('IntegerValue()', function () {
        it('should return 1234 for "1234"', function () {
            assert.strictEqual(module.Value_IntegerValue("1234"), 1234n);
        });
        it('should return 1 for true', function () {
            assert.strictEqual(module.Value_IntegerValue(true), 1n);
        });
        it('should return 0 for {}', function () {
            assert.strictEqual(module.Value_IntegerValue({}), 0n);
        });
        it('should return 1234 for "1234.5"', function () {
            assert.strictEqual(module.Value_IntegerValue("1234.5"), 1234n);
        });
        it('should return 3 for 3.1415', function () {
            assert.strictEqual(module.Value_IntegerValue(3.1415), 3n);
        });
        it('should return 0 for NaN', function () {
            assert.strictEqual(module.Value_IntegerValue(NaN), 0n);
        });
        it('should return Nothing when exception is thrown', function () {
            var o = { [Symbol.toPrimitive]: function() { throw new Error(); } };
            assert.strictEqual(module.Value_IntegerValue(o), undefined);
        });
        it('should return the correct value when called with a pending exception', function () {
            assert.strictEqual(module.Value_IntegerValuePendingException("1234"), 1234n);
            assert.strictEqual(module.Value_IntegerValuePendingException(null), 0n);
            assert.strictEqual(module.Value_IntegerValuePendingException(true), 1n);
        });
    });
    describe('NumberValue()', function () {
        it('should return NaN for undefined', function () {
            assert.strictEqual(isNaN(module.Value_NumberValue(undefined)), true);
        });
        it('should return 0 for null', function () {
            assert.strictEqual(module.Value_NumberValue(null), 0);
        });
        it('should return 1234 for "1234"', function () {
            assert.strictEqual(module.Value_NumberValue("1234"), 1234);
        });
        it('should return 1 for true', function () {
            assert.strictEqual(module.Value_NumberValue(true), 1);
        });
        it('should return 0 for false', function () {
            assert.strictEqual(module.Value_NumberValue(false), 0);
        });
        it('should return NaN for {}', function () {
            assert.strictEqual(isNaN(module.Value_NumberValue({})), true);
        });
        it('should return 1234.5 for "1234.5"', function () {
            assert.strictEqual(module.Value_NumberValue("1234.5"), 1234.5);
        });
        it('should return 3.1415 for 3.1415', function () {
            assert.strictEqual(module.Value_NumberValue(3.1415), 3.1415);
        });
        it('should return milliseconds for date', function () {
            var date = new Date();
            assert.strictEqual(module.Value_NumberValue(date), date.valueOf());
        });
        it('should return Nothing when exception is thrown', function () {
            var o = { [Symbol.toPrimitive]: function() { throw new Error(); } };
            assert.strictEqual(module.Value_NumberValue(o), undefined);
        });
        it('should return the correct value when called with a pending exception', function () {
            assert.strictEqual(module.Value_NumberValuePendingException("1234"), 1234);
            assert.strictEqual(module.Value_NumberValuePendingException(null), 0);
            assert.strictEqual(module.Value_NumberValuePendingException(true), 1);
        });
    });
    describe('BooleanValue()', function () {
        it('should return false for undefined', function () {
            assert.strictEqual(module.Value_BooleanValue(undefined), false);
        });
        it('should return false for null', function () {
            assert.strictEqual(module.Value_BooleanValue(null), false);
        });
        it('should return true for "1234"', function () {
            assert.strictEqual(module.Value_BooleanValue("1234"), true);
        });
        it('should return true for true', function () {
            assert.strictEqual(module.Value_BooleanValue(true), true);
        });
        it('should return false for false', function () {
            assert.strictEqual(module.Value_BooleanValue(false), false);
        });
        it('should return false for {}', function () {
            assert.strictEqual(isNaN(module.Value_BooleanValue({})), false);
        });
        it('should return true for "1234.5"', function () {
            assert.strictEqual(module.Value_BooleanValue("1234.5"), true);
        });
        it('should return false for 0', function () {
            assert.strictEqual(module.Value_BooleanValue(0), false);
        });
        it('should return true for 3.1415', function () {
            assert.strictEqual(module.Value_BooleanValue(3.1415), true);
        });
    });
    describe('Int32Value()', function () {
        it('should return 0 for undefined', function () {
            assert.strictEqual(module.Value_Int32Value(undefined), 0);
        });
        it('should return 0 for null', function () {
            assert.strictEqual(module.Value_Int32Value(null), 0);
        });
        it('should return 1234 for "1234"', function () {
            assert.strictEqual(module.Value_Int32Value("1234"), 1234);
        });
        it('should return 1 for true', function () {
            assert.strictEqual(module.Value_Int32Value(true), 1);
        });
        it('should return 0 for false', function () {
            assert.strictEqual(module.Value_Int32Value(false), 0);
        });
        it('should return 0 for {}', function () {
            assert.strictEqual(module.Value_Int32Value({}), 0);
        });
        it('should return 1234 for "1234.5"', function () {
            assert.strictEqual(module.Value_Int32Value("1234.5"), 1234);
        });
        it('should return 0 for 0', function () {
            assert.strictEqual(module.Value_Int32Value(0), 0);
        });
        it('should return 3 for 3.1415', function () {
            assert.strictEqual(module.Value_Int32Value(3.1415), 3);
        });
        it('should return Math.pow(2,31)-1 for Math.pow(2,31)-1', function () {
            var maxVal = Math.pow(2, 31) - 1;
            assert.strictEqual(module.Value_Int32Value(maxVal), maxVal);
        });
        it('should return -Math.pow(2,31) for Math.pow(2,31)', function () {
            var maxVal = Math.pow(2, 31);
            assert.strictEqual(module.Value_Int32Value(maxVal), -maxVal);
        });
        it('should return Nothing when exception is thrown', function () {
            var o = { [Symbol.toPrimitive]: function() { throw new Error(); } };
            assert.strictEqual(module.Value_Int32Value(o), undefined);
        });
        it('should return the correct value when called with a pending exception', function () {
            assert.strictEqual(module.Value_Int32ValuePendingException("1234"), 1234);
            assert.strictEqual(module.Value_Int32ValuePendingException(null), 0);
            assert.strictEqual(module.Value_Int32ValuePendingException(true), 1);
        });
    });
    describe('Uint32Value()', function () {
        it('should return 0 for undefined', function () {
            assert.strictEqual(module.Value_Uint32Value(undefined), 0);
        });
        it('should return 0 for null', function () {
            assert.strictEqual(module.Value_Uint32Value(null), 0);
        });
        it('should return 1234 for "1234"', function () {
            assert.strictEqual(module.Value_Uint32Value("1234"), 1234);
        });
        it('should return 1 for true', function () {
            assert.strictEqual(module.Value_Uint32Value(true), 1);
        });
        it('should return 0 for false', function () {
            assert.strictEqual(module.Value_Uint32Value(false), 0);
        });
        it('should return 0 for {}', function () {
            assert.strictEqual(module.Value_Uint32Value({}), 0);
        });
        it('should return 1234 for "1234.5"', function () {
            assert.strictEqual(module.Value_Uint32Value("1234.5"), 1234);
        });
        it('should return 0 for 0', function () {
            assert.strictEqual(module.Value_Uint32Value(0), 0);
        });
        it('should return 3 for 3.1415', function () {
            assert.strictEqual(module.Value_Uint32Value(3.1415), 3);
        });
        it('should return Math.pow(2,32)-1 for Math.pow(2,32)-1', function () {
            var maxVal = Math.pow(2, 32) - 1;
            assert.strictEqual(module.Value_Uint32Value(maxVal), maxVal);
        });
        it('should return 0 for Math.pow(2,32)', function () {
            var maxVal = Math.pow(2, 32);
            assert.strictEqual(module.Value_Uint32Value(maxVal), 0);
        });
        it('should return Nothing when exception is thrown', function () {
            var o = { [Symbol.toPrimitive]: function() { throw new Error(); } };
            assert.strictEqual(module.Value_Uint32Value(o), undefined);
        });
        it('should return the correct value when called with a pending exception', function () {
            assert.strictEqual(module.Value_Uint32ValuePendingException("1234"), 1234);
            assert.strictEqual(module.Value_Uint32ValuePendingException(null), 0);
            assert.strictEqual(module.Value_Uint32ValuePendingException(true), 1);
        });
    });
});

describe('Value - To*()', function () {
    describe('ToBoolean', function () {
        it('should return true for several primitive values', function () {
            assert.strictEqual(module.Value_ToBoolean(true), true);
            assert.strictEqual(module.Value_ToBoolean(1), true);
            assert.strictEqual(module.Value_ToBoolean("1"), true);
            assert.strictEqual(module.Value_ToBoolean("0"), true);
            assert.strictEqual(module.Value_ToBoolean("true"), true);
            assert.strictEqual(module.Value_ToBoolean(true), true);
        });
        it('should return false for several primitive values', function () {
            assert.strictEqual(module.Value_ToBoolean(false), false);
            assert.strictEqual(module.Value_ToBoolean(null), false);
            assert.strictEqual(module.Value_ToBoolean(undefined), false);
            assert.strictEqual(module.Value_ToBoolean(0), false);
            assert.strictEqual(module.Value_ToBoolean(""), false);
            assert.strictEqual(module.Value_ToBoolean("false"), true);
        });
        it('should return true for symbols', function () {
            assert.strictEqual(module.Value_ToBoolean(Symbol("test")), true);
        });
        it('should return true for objects', function () {
            assert.strictEqual(module.Value_ToBoolean({}), true);
        });
    });
    describe('ToNumber', function () {
        it('should return NaN for undefined', function () {
            assert.strictEqual(isNaN(module.Value_ToNumber(undefined)), true);
        });
        it('should return 0 for null', function () {
            assert.strictEqual(module.Value_ToNumber(null), 0);
        });
        it('should work for boolean input', function () {
            assert.strictEqual(module.Value_ToNumber(true), 1);
            assert.strictEqual(module.Value_ToNumber(false), 0);
        });
        it('should work for numeric input', function () {
            assert.strictEqual(module.Value_ToNumber(-0.0), -0.0);
            var maxVal = Math.pow(2, 32);
            assert.strictEqual(module.Value_ToNumber(maxVal), maxVal);
            assert.strictEqual(module.Value_ToNumber(Math.PI), Math.PI);
        });
        it('should work for string input', function () {
            assert.strictEqual(module.Value_ToNumber("3.1415"), 3.1415);
            assert.strictEqual(module.Value_ToNumber("-123456789"), -123456789);
        });
        it('should throw for symbol input', function () {
            assert.throws(function () {
                module.Value_ToNumber(Symbol("test"));
            }, TypeError);
        });
        it('should work for object input', function () {
            var obj = {valueOf: function () {
                    return 3.1415;
                }};
            assert.strictEqual(module.Value_ToNumber(obj), 3.1415);
        });
    });
    describe('ToString', function () {
        it('should return "undefined" for undefined', function () {
            assert.strictEqual(module.Value_ToString(undefined), "undefined");
        });
        it('should return "null" for null', function () {
            assert.strictEqual(module.Value_ToString(null), "null");
        });
        it('should work for boolean input', function () {
            assert.strictEqual(module.Value_ToString(true), "true");
            assert.strictEqual(module.Value_ToString(false), "false");
        });
        it('should work for number input', function () {
            assert.strictEqual(module.Value_ToString(3.1415), "3.1415");
        });
        it('should throw for symbol input', function () {
            assert.throws(function () {
                module.Value_ToString(Symbol("test"));
            }, TypeError);
        });
        it('should work for object input', function () {
            var obj = {toString: function () {
                    return "3.1415";
                }};
            assert.strictEqual(module.Value_ToString(obj), "3.1415");
        });
    });
    describe('ToInteger', function () {
        it('should call ToNumber', function () {
            assert.strictEqual(module.Value_ToInteger("+3"), 3);
        });
        it('should return 0 for NaN', function () {
            assert.strictEqual(module.Value_ToInteger(NaN), 0);
            assert.strictEqual(module.Value_ToInteger(undefined), 0);
        });
        it('should return 0 for positive/negative zero', function () {
            assert.strictEqual(module.Value_ToInteger(0), 0);
            assert.strictEqual(module.Value_ToInteger(-0.0), 0);
        });
        it('should not convert corner cases', function () {
            assert.strictEqual(module.Value_ToInteger(Number.POSITIVE_INFINITY), Number.POSITIVE_INFINITY);
            assert.strictEqual(module.Value_ToInteger(Number.NEGATIVE_INFINITY), Number.NEGATIVE_INFINITY);
            assert.strictEqual(module.Value_ToInteger(0xFFFFFFFF), 0xFFFFFFFF);
            assert.strictEqual(module.Value_ToInteger(1e100), 1e100);
        });
    });
    describe('ToInt32', function () {
        it('should call ToNumber', function () {
            assert.strictEqual(module.Value_ToInt32("+3"), 3);
        });
        it('should convert corner cases', function () {
            assert.strictEqual(module.Value_ToInt32(0), 0);
            assert.strictEqual(module.Value_ToInt32(-0.0), 0);
            assert.strictEqual(module.Value_ToInt32(NaN), 0);
            assert.strictEqual(module.Value_ToInt32(Number.POSITIVE_INFINITY), 0);
            assert.strictEqual(module.Value_ToInt32(Number.NEGATIVE_INFINITY), 0);
        });
        it('should not convert negatives', function () {
            var two31 = Math.pow(2, 31);
            assert.strictEqual(module.Value_ToInt32(-1), -1);
            assert.strictEqual(module.Value_ToInt32(-2), -2);
            assert.strictEqual(module.Value_ToInt32(-two31), -two31);
        });
        it('should convert outside range', function () {
            var two31 = Math.pow(2, 31);
            assert.strictEqual(module.Value_ToInt32(two31 + 1), -two31 + 1);
        });
    });
    describe('ToUint32', function () {
        it('should call ToNumber', function () {
            assert.strictEqual(module.Value_ToUint32("+3"), 3);
        });
        it('should convert corner cases', function () {
            assert.strictEqual(module.Value_ToUint32(0), 0);
            assert.strictEqual(module.Value_ToUint32(-0.0), 0);
            assert.strictEqual(module.Value_ToUint32(NaN), 0);
            assert.strictEqual(module.Value_ToUint32(Number.POSITIVE_INFINITY), 0);
            assert.strictEqual(module.Value_ToUint32(Number.NEGATIVE_INFINITY), 0);
        });
        it('should convert outside range', function () {
            var two32 = Math.pow(2, 32);
            assert.strictEqual(module.Value_ToUint32(two32), 0);
            assert.strictEqual(module.Value_ToUint32(two32 + 1), 1);
        });
        it('should convert outside range', function () {
            var two32 = Math.pow(2, 32);
            assert.strictEqual(module.Value_ToUint32(-1), two32 - 1);
            assert.strictEqual(module.Value_ToUint32(-2), two32 - 2);
        });
    });
    describe('ToArrayIndex', function () {
        it('should be an identity on small non-negative numbers', function () {
            assert.strictEqual(module.Value_ToArrayIndex(0), 0);
            assert.strictEqual(module.Value_ToArrayIndex(1), 1);
        });
        it('should return an empty handle for non-numerical values', function () {
            // Value_ToArrayIndex returns undefined when ToArrayIndex is an empty handle
            assert.strictEqual(module.Value_ToArrayIndex(true), undefined);
            assert.strictEqual(module.Value_ToArrayIndex(undefined), undefined);
            assert.strictEqual(module.Value_ToArrayIndex(null), undefined);
            assert.strictEqual(module.Value_ToArrayIndex({}), undefined);
            assert.strictEqual(module.Value_ToArrayIndex("zero"), undefined);
        });
        it('should return an empty handle for negative values', function () {
            assert.strictEqual(module.Value_ToArrayIndex(-1), undefined);
            assert.strictEqual(module.Value_ToArrayIndex(-0.5), undefined);
        });
        it('should return an empty handle for non-integer numbers', function () {
            assert.strictEqual(module.Value_ToArrayIndex(3.14), undefined);
        });
        it('should return zero for a negative zero', function () {
            assert.strictEqual(module.Value_ToArrayIndex(-0.0), 0);
        });
        it('should return an empty handle for NaN', function () {
            assert.strictEqual(module.Value_ToArrayIndex(NaN), undefined);
        });
        it('should return an empty handle for positive and negative infinity', function () {
            assert.strictEqual(module.Value_ToArrayIndex(Number.POSITIVE_INFINITY), undefined);
            assert.strictEqual(module.Value_ToArrayIndex(Number.NEGATIVE_INFINITY), undefined);
        });
        it('should return number for a string that represents an non-negative integer', function () {
            assert.strictEqual(module.Value_ToArrayIndex("0"), 0);
            assert.strictEqual(module.Value_ToArrayIndex("123"), 123);
        });
        it('should respect that the maximum array index is 2^32-2', function () {
            var two32 = Math.pow(2, 32);
            assert.strictEqual(module.Value_ToArrayIndex(two32 - 1), undefined);
            assert.strictEqual(module.Value_ToArrayIndex(two32 - 2), two32 - 2);
        });
    });
    describe('ToObject', function () {
        it('should convert primitives', function () {
            assert.strictEqual(typeof module.Value_ToObject(true), "object");
            assert.strictEqual(typeof module.Value_ToObject(1), "object");
            assert.strictEqual(typeof module.Value_ToObject(Math.PI), "object");
            assert.strictEqual(typeof module.Value_ToObject("test_str"), "object");
        });
        it('should pass objects', function () {
            var obj = {a: 345};
            assert.strictEqual(module.Value_ToObject(obj) === obj, true);
        });
        it('should convert primitive symbols', function () {
            assert.strictEqual(typeof module.Value_ToObject(Symbol("test")), "object");
        });
        it('should throw for null and undefined', function () {
            assert.throws(function () {
                module.Value_ToObject(null);
            }, TypeError);
            assert.throws(function () {
                module.Value_ToObject(undefined);
            }, TypeError);
        });
    });
});

describe('Value - other methods', function () {
    describe('Equals', function () {
        it('should return true for \'\' and false', function () {
            assert.strictEqual(module.Value_Equals('', false), true);
        });
        it('should return true for undefined and null', function () {
            assert.strictEqual(module.Value_Equals(undefined, null), true);
        });
        it('should return false for 2 and true', function () {
            assert.strictEqual(module.Value_Equals(2, true), false);
        });
    });
    describe('StrictEquals', function () {
        it('should return false for NaN and NaN', function () {
            assert.strictEqual(module.Value_StrictEquals(NaN, NaN), false);
        });
        it('should return false for undefined and null', function () {
            assert.strictEqual(module.Value_StrictEquals(undefined, null), false);
        });
        it('should return true for +0 and -0', function () {
            assert.strictEqual(module.Value_StrictEquals(+0, -0), true);
        });
        it('should return true for "hello" and "hell"+"o"', function () {
            assert.strictEqual(module.Value_StrictEquals("hello", "hell" + "o"), true);
        });
        it('should return false for {} and {} (two different objects)', function () {
            assert.strictEqual(module.Value_StrictEquals({}, {}), false);
        });
        it('should return true for {} and {} (two references to the same object)', function () {
            var o = {};
            assert.strictEqual(module.Value_StrictEquals(o, o), true);
        });
    });
    describe('TypeOf', function () {
        it('should return "undefined" for undefined', function () {
            assert.strictEqual(module.Value_TypeOf(undefined), "undefined");
        });
        it('should return "object" for null', function () {
            assert.strictEqual(module.Value_TypeOf(null), "object");
        });
        it('should return "boolean" for true and false', function () {
            assert.strictEqual(module.Value_TypeOf(true), "boolean");
            assert.strictEqual(module.Value_TypeOf(false), "boolean");
        });
        it('should return "number" for 42, NaN and Infinity', function () {
            assert.strictEqual(module.Value_TypeOf(42), "number");
            assert.strictEqual(module.Value_TypeOf(NaN), "number");
            assert.strictEqual(module.Value_TypeOf(Infinity), "number");
        });
        it('should return "bigint" for 42n', function () {
            assert.strictEqual(module.Value_TypeOf(42n), "bigint");
        });
        it('should return "symbol" for Symbol()', function () {
            assert.strictEqual(module.Value_TypeOf(Symbol()), "symbol");
        });
        it('should return "string" for "foo"', function () {
            assert.strictEqual(module.Value_TypeOf("foo"), "string");
        });
        it('should return "object" for "JSON"', function () {
            assert.strictEqual(module.Value_TypeOf(JSON), "object");
        });
        it('should return "function" for an arrow function', function () {
            assert.strictEqual(module.Value_TypeOf(() => 42), "function");
        });
        it('should return "function" for a callable proxy', function () {
            assert.strictEqual(module.Value_TypeOf(new Proxy(function() {}, {})), "function");
        });
        it('should return "function" for a revoked callable proxy', function () {
            var r = Proxy.revocable(function() {}, {});
            r.revoke();
            assert.strictEqual(module.Value_TypeOf(r.proxy), "function");
        });
    });
    describe('ToDetailString', function () {
        it('should be identity for strings', function () {
            assert.strictEqual(module.Value_ToDetailString("foo"), "foo");
        });
        it('should work for symbols', function () {
            assert.strictEqual(module.Value_ToDetailString(Symbol.toPrimitive), "Symbol(Symbol.toPrimitive)");
        });
        it('should work for proxies', function () {
            module.Value_ToDetailString(new Proxy(function() {}, {}));
        });
        it('should have no side-effects', function () {
            var sideEffect = false;
            var o = {
                toString() {
                    sideEffect = true;
                },
                valueOf() {
                    sideEffect = true;
                }
            };
            module.Value_ToDetailString(o);
            assert.ok(!sideEffect);
        });
    });
});
