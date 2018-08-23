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
var v8 = require('v8');

var arrayBuffer = new ArrayBuffer(2);
var typedArray = new Uint8Array(arrayBuffer);
typedArray[0] = 42;
typedArray[1] = 211;
var map = new Map();
map.set(42, 211);
var set = new Set();
set.add(42);
var cyclic = {};
cyclic.me = cyclic;
var sparseArray = new Array(1000);
sparseArray[42] = 211;
var sparseArrayWithProperty = new Array(1000);
sparseArrayWithProperty[42] = 211;
sparseArrayWithProperty.foo = 'bar';
var denseArrayWithProperty = [42, 211];
denseArrayWithProperty.foo = 'bar';
var sparseArrayWithHighIndex = new Array(4294967295);
sparseArrayWithHighIndex[4294967294] = 0;

var data = [
    [0, 'ff0d4900'],
    [-0, 'ff0d4e0000000000000080'],
    [1, 'ff0d4902'],
    [-1, 'ff0d4901'],
    [2147483647, 'ff0d49feffffff0f'],
    [2147483648, 'ff0d4e000000000000e041'],
    [-2147483648, 'ff0d49ffffffff0f'],
    [-2147483649, 'ff0d4e000020000000e0c1'],
    [Infinity, 'ff0d4e000000000000f07f'],
    [-Infinity, 'ff0d4e000000000000f0ff'],
    [true, 'ff0d54'],
    [false, 'ff0d46'],
    [undefined, 'ff0d5f'],
    [null, 'ff0d30'],
    [Object(true), 'ff0d79'],
    [Object(false), 'ff0d78'],
    ['', 'ff0d2200'],
    ['one byte string', 'ff0d220f6f6e65206279746520737472696e67'],
    ['two byte štring', 'ff0d631e740077006f0020006200790074006500200061017400720069006e006700'],
    [new Date(1533221094604), 'ff0d4400c04c94b14f7642'],
    [Object(1), 'ff0d6e000000000000f03f'],
    [Object('a'), 'ff0d73220161'],
    [/^.*$/m, 'ff0d5222045e2e2a2404'],
    [new ArrayBuffer(), 'ff0d4200'],
    [arrayBuffer, 'ff0d42022ad3'],
    [new DataView(new ArrayBuffer()), 'ff0d5c0900'],
    [new Int8Array(), 'ff0d5c0000'],
    [new Uint8Array(), 'ff0d5c0100'],
    [new Uint8ClampedArray(), 'ff0d5c0200'],
    [new Int16Array(), 'ff0d5c0300'],
    [new Uint16Array(), 'ff0d5c0400'],
    [new Int32Array(), 'ff0d5c0500'],
    [new Uint32Array(), 'ff0d5c0600'],
    [new Float32Array(), 'ff0d5c0700'],
    [new Float64Array(), 'ff0d5c0800>'],
    [typedArray, 'ff0d5c01022ad3'],
    [new Map(), 'ff0d3b3a00'],
    [new Set(), 'ff0d272c00'],
    [map, 'ff0d3b495449a6033a02'],
    [set, 'ff0d2749542c01'],
    [{}, 'ff0d6f7b00'],
    [{ foo: 'bar' }, 'ff0d6f2203666f6f22036261727b01'],
    [cyclic, 'ff0d6f22026d655e007b01'],
    [[], 'ff0d4100240000'],
    [[42,211], 'ff0d4102495449a603240002'],
    [new Array(4294967295), 'ff0d61ffffffff0f4000ffffffff0f'],
    [sparseArray, 'ff0d61e807495449a6034001e807'],
    [sparseArrayWithProperty, 'ff0d61e807495449a6032203666f6f22036261724002e807'],
    [denseArrayWithProperty, 'ff0d4102495449a6032203666f6f2203626172240102'],
    [sparseArrayWithHighIndex, 'ff0d61ffffffff0f4e0000c0ffffffef4149004001ffffffff0f']
];

describe('Serialization', function () {
    it('should produce the same result as the original Node.js', function () {
        for (var pair of data) {
            assert.deepEqual(v8.serialize(pair[0]), Buffer.from(pair[1], 'hex'), pair[0]);
        }
    });
    it('should deserialize the serialized object', function() {
        for (var pair of data) {
            var serialized = pair[0];
            var deserialized = v8.deserialize(v8.serialize(serialized));
            assert.deepEqual(deserialized, serialized, serialized);
        }
    });
    it('should keep identity of objects seralized multiple times', function () {
        for (var pair of data) {
            var object = pair[0];
            var serialized = { ref1: object, ref2: object };
            var deserialized = v8.deserialize(v8.serialize(serialized));
            assert.deepEqual(deserialized.ref1, object, object);
            assert.deepEqual(deserialized.ref2, object, object);
            assert.strictEqual(deserialized.ref1, deserialized.ref2, object);
        }
    });
    it('should keep track of IDs of serialized objects', function () {
        var object = { top: 'secret' };
        var serialized = {
            // serialize all kinds of objects before serializing
            // the tested object references (to verify that none
            // of these objects desynchronizes the counting/IDs
            // of serialized/deserialized objects)
            data : data,
            ref1 : object,
            ref2 : object
        }
        var deserialized = v8.deserialize(v8.serialize(serialized));
        assert.strictEqual(deserialized.ref1, deserialized.ref2);
        assert.deepEqual(deserialized.ref1, object);
        assert.deepEqual(deserialized.ref2, object);
    })
});
