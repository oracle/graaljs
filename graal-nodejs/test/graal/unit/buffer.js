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

function expectedErrorValidator(ExpectedError, expectedCode, expectedMessage) {
    return (err) => {
        assert(err instanceof ExpectedError, `The error is expected to be an instance of ${ExpectedError.name}. Received ${err?.constructor?.name}.`);
        assert.strictEqual(err.code, expectedCode);
        if (expectedMessage !== undefined) {
            assert.strictEqual(err.message, expectedMessage);
        }
        return true;
    };
}

const RangeErrorOutOfRange = expectedErrorValidator(RangeError, "ERR_OUT_OF_RANGE", "Index out of range");
const RangeErrorBufferOutOfBounds = expectedErrorValidator(RangeError, "ERR_BUFFER_OUT_OF_BOUNDS", '"offset" is outside of buffer bounds');
const TypeErrorNotBuffer = expectedErrorValidator(TypeError, "ERR_INVALID_ARG_TYPE", "argument must be a buffer");
const TypeErrorNotString = expectedErrorValidator(TypeError, "ERR_INVALID_ARG_TYPE", "argument must be a string");

const TypedArrays = [
    Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array,
    Float32Array, Float64Array, BigInt64Array, BigUint64Array
];

describe('Buffer.utf8Write', function() {
    it('should write if length is >> than input string', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('abc', 0, 100), 3);
    });
    it('should fail with negative offset', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Write('abc', -5, 1)
        }, RangeErrorBufferOutOfBounds);
    });
    it('should use string length as default argument', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('abc'), 3);
    });
    it('should use string length as default argument #2', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('abc', 0), 3);
    });
    it('should fail if string len is negative', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Write('abc', 0, -1)
        }, expectedErrorValidator(RangeError, "ERR_BUFFER_OUT_OF_BOUNDS", '"length" is outside of buffer bounds'));
    });
    it('should fail with out of bounds offset', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Write('abc', 11, 1)
        }, RangeErrorBufferOutOfBounds);
    });
    it('should fail with out of bounds offset #2', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Write('abc', 11, -1)
        }, RangeErrorBufferOutOfBounds);
    });
    it('should report tot bytes for utf8 values', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('½½½', 0, 2), 2);
    });
    it('should fail if argument is not a string', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Write(1)
        }, TypeErrorNotString);
    });
    it('should accept zero range', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('abc', 0, 0), 0);
    });
    it('should handle offset correctly', function() {
        assert.strictEqual(Buffer.from(new ArrayBuffer(20), 0, 10).utf8Write('abcdefghi', 5, 9), 5);
    });
    it('should write correct buffer size', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('abcdefghijklmnopqrstuvwxyz', true), 9);
    });
    it('should expect typed arrays', function() {
        assert.throws(() => {
            Buffer.prototype.utf8Write.call('buffer', 'text to write')
        }, TypeErrorNotBuffer);
        TypedArrays.forEach(TypedArray => Buffer.prototype.utf8Write.call(new TypedArray(13), 'text to write'));
    });
    it('should deal with utf8 inputs', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('½½', 0, 3), 2);
    });
    it('should deal with utf8 inputs #2', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('½½½'), 6);
    });
    it('should handle int overflow', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Write('a', 1, 0xffffffff), 1);
    });
    it('length is zero', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Write.length, 1);
    });
    if (module.hasJavaInterop()) {
        it('should accept interop buffer', function() {
            var byteLength = 8;
            var offset = 4;
            var array = new Uint8Array(new ArrayBuffer(byteLength), offset);
            var interopArray = new Uint8Array(new ArrayBuffer(java.nio.ByteBuffer.allocate(byteLength)), offset);
            var text = 'foo';
            Buffer.prototype.utf8Write.call(array, text, 1, 2);
            Buffer.prototype.utf8Write.call(interopArray, text, 1, 2);
            for (var i = 0; i < byteLength; i++) {
                assert.strictEqual(interopArray[i], array[i]);
            }
        });
    }
});

describe('Buffer.utf8Slice', function() {
    it('should convert arguments', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Slice(false, true), '\u0000');
    });
    it('should check range', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Slice(-1, 10)
        }, RangeErrorOutOfRange);
    });
    it('should check range #2', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Slice(11, 10)
        }, RangeErrorOutOfRange);
    });
    it('should check range #3', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Slice(0, -1)
        }, RangeErrorOutOfRange);
    });
    it('should check range #4', function() {
        assert.throws(() => {
            Buffer.alloc(10).utf8Slice(0, 11)
        }, RangeErrorOutOfRange);
    });
    it('should slice the full buffer with no arguments', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Slice().length, 10);
    });
    it('should slice using the proper default value', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Slice(5).length, 5);
    });
    it('should allow end < start', function() {
        assert.strictEqual(Buffer.alloc(10).utf8Slice(5, 4), '');
    });
    it('should return an empty string', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Slice(-5, -10), '');
    });
    it('should return an empty string #2', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Slice(20, 10), '');
    });
    it('should return an empty string #3', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Slice('hi', 'there'), '');
    });
    it('should return an empty string #4', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Slice(true, false), '');
    });
    it('should return an empty string #5', function() {
        let badValue = {valueOf() { throw new TypeError("valueOf() should not be called"); }};
        assert.strictEqual(Buffer.alloc(0).utf8Slice(badValue, badValue), '');
    });
    it('should check buffer type', function() {
        assert.throws(() => {
            Buffer.prototype.utf8Slice.call(1)
        }, TypeErrorNotBuffer);
        TypedArrays.forEach(TypedArray => Buffer.prototype.utf8Slice.call(new TypedArray()));
    });
    it('length is zero', function() {
        assert.strictEqual(Buffer.alloc(0).utf8Slice.length, 0);
    });
    it('should replace invalid UTF-8 bytes with replacement characters', function() {
        let invalid = [
            // Impossible bytes (0xc0, 0xc1, and 0xf5..0xff)
            "fe",
            "ff",
            "c0c1",
            "f5f6f7f8f9fafbfcfdfeff",
            // Unexpected continuation bytes
            "80",
            "bf",
            "80bf",
            "80bf80",
            "80bf80bf",
        ];
        for (let hex of invalid) {
            let buffer = Buffer.from(hex, "hex");
            assert.strictEqual(buffer.utf8Slice(), '\ufffd'.repeat(buffer.byteLength), hex);
        }
    });
    it('should replace valid incomplete UTF-8 sequences with a single replacement character', function() {
        let incompleteSeqs = [
            [ // C2..DF: 2-byte sequence with last byte missing
                ["c080", '\ufffd\ufffd'],
                ["c0bf", '\ufffd\ufffd'],
                ["c180", '\ufffd\ufffd'],
                ["c1bf", '\ufffd\ufffd'],
                ["c2"],
                ["df"],
            ],
            [ // E0..EF: 3-byte sequence with last byte missing
                ["e080", '\ufffd\ufffd'],
                ["e08f", '\ufffd\ufffd'],
                ["e0a0"], // If byte is 0xE0, set UTF-8 lower boundary to 0xA0.
                ["e0bf"],
                ["ed80"],
                ["ed9f"], // If byte is 0xED, set UTF-8 upper boundary to 0x9F.
                ["eda0", '\ufffd\ufffd'],
                ["edbf", '\ufffd\ufffd'],
            ].concat([..."123456789abcef"].flatMap(x => [
                [`e${x}80`],
                [`e${x}bf`],
            ])),
            [ // F0..F4: 4-byte sequence with last byte missing
                ["f08fbf", '\ufffd\ufffd\ufffd'],
                ["f09080"], // If byte is 0xF0, set UTF-8 lower boundary to 0x90.
                ["f0bfbf"],
                ["f18080"],
                ["f1bfbf"],
                ["f28080"],
                ["f2bfbf"],
                ["f38080"],
                ["f3bfbf"],
                ["f48080"],
                ["f48fbf"], // If byte is 0xF4, set UTF-8 upper boundary to 0x8F.
                ["f49080", '\ufffd\ufffd\ufffd'],
            ].flatMap(p => [p, p.map(s => s.substring(0, s.length / 3 * 2))]),
        ].reduce((a, b) => a.concat(b));

        for (let [utf8Hex, utf16 = '\ufffd'] of incompleteSeqs) {
            assert.strictEqual(Buffer.from(utf8Hex, "hex").utf8Slice(), utf16, utf8Hex);

            // followed by valid character
            assert.strictEqual(Buffer.from(utf8Hex + "78", "hex").utf8Slice(), utf16 + "x", utf8Hex + "78");
        }
    });
    it('should replace UTF-8 encoded UTF-16 surrogate bytes with replacement characters', function() {
        let singleSurrogateSeqs = [
            "eda080", // U+D800
            "edadbf", // U+DB7F
            "edae80", // U+DB80
            "edafbf", // U+DBFF
            "edb080", // U+DC00
            "edbe80", // U+DF80
            "edbfbf", // U+DFFF
        ];
        let pairedSurrogateSeqs = [
            "eda080edb080", // U+D800 U+DC00
            "eda080edbfbf", // U+D800 U+DFFF
            "edadbfedb080", // U+DB7F U+DC00
            "edadbfedbfbf", // U+DB7F U+DFFF
            "edae80edb080", // U+DB80 U+DC00
            "edae80edbfbf", // U+DB80 U+DFFF
            "edafbfedb080", // U+DBFF U+DC00
            "edafbfedbfbf", // U+DBFF U+DFFF
        ];
        for (let hex of singleSurrogateSeqs.concat(pairedSurrogateSeqs)) {
            let buffer = Buffer.from(hex, "hex");
            assert.strictEqual(buffer.utf8Slice(), '\ufffd'.repeat(buffer.byteLength), hex);
        }
    });
    it('should replace overlong UTF-8 sequence bytes with replacement characters', function() {
        let overlongSeqs = [
            "c080",     // U+0000
            "e08080",   // U+0000
            "f0808080", // U+0000
            "c0af",     // U+002F
            "e080af",   // U+002F
            "f08080af", // U+002F
            "c1bf",     // U+007F
            "e09fbf",   // U+07FF
            "f08fbfbf", // U+FFFF
        ];
        for (let hex of overlongSeqs) {
            let buffer = Buffer.from(hex, "hex");
            assert.strictEqual(buffer.utf8Slice(), '\ufffd'.repeat(buffer.byteLength), hex);
        }
    });
    it('should decode noncharacters', function() {
        let pairs = [
            ["efbfbe", "\ufffe"],
            ["efbfbf", "\uffff"],
        ];
        for (let [utf8Hex, utf16] of pairs) {
            let buffer = Buffer.from(utf8Hex, "hex");
            assert.strictEqual(buffer.utf8Slice(), utf16, utf8Hex);
        }
    });
    if (module.hasJavaInterop()) {
        it('should accept interop buffer', function() {
            var javaBuffer = java.nio.ByteBuffer.allocate(8);
            javaBuffer.put(3, 'f'.codePointAt(0));
            javaBuffer.put(4, 'o'.codePointAt(0));
            javaBuffer.put(5, 'o'.codePointAt(0));
            var array = new Uint8Array(new ArrayBuffer(javaBuffer), 2);
            var result = Buffer.prototype.utf8Slice.call(array, 1, 4);
            assert.strictEqual(result, 'foo');
        });
    }
});
