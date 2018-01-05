/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Buffer.utf8Write', function() {
    it('should write if length is >> than input string', function() {
        assert.strictEqual(new Buffer(10).utf8Write('abc', 0, 100), 3);
    });
    it('should fail with negative offset', function() {
        assert.throws(() => {
            new Buffer(10).utf8Write('abc', -5, 1)
        }, RangeError);
    });
    it('should use string length as default argument', function() {
        assert.strictEqual(new Buffer(10).utf8Write('abc'), 3);
    });
    it('should use string length as default argument #2', function() {
        assert.strictEqual(new Buffer(10).utf8Write('abc', 0), 3);
    });
    it('should fail if string len is negative', function() {
        assert.throws(() => {
            new Buffer(10).utf8Write('abc', 0, -1)
        }, RangeError);
    });
    it('should report tot bytes for utf8 values', function() {
        assert.strictEqual(new Buffer(10).utf8Write('½½½', 0, 2), 2);
    });
    it('should fail if argument is not a string', function() {
        assert.throws(() => {
            new Buffer(10).utf8Write(1)
        }, TypeError);
    });
    it('should accept zero range', function() {
        assert.strictEqual(new Buffer(10).utf8Write('abc', 0, 0), 0);
    });
    it('should handle offset correctly', function() {
        assert.strictEqual(new Buffer(new ArrayBuffer(20), 0, 10).utf8Write('abcdefghi', 5, 9), 5);
    });
    it('should write correct buffer size', function() {
        assert.strictEqual(new Buffer(10).utf8Write('abcdefghijklmnopqrstuvwxyz', true), 9);
    });
    it('should expect typed arrays', function() {
        assert.throws(() => {
            Buffer.prototype.utf8Write.call('buffer', 'text to write')
        }, TypeError);
    });
    it('should deal with utf8 inputs', function() {
        assert.strictEqual(new Buffer(10).utf8Write('½½', 0, 3), 2);
    });
    it('should deal with utf8 inputs #2', function() {
        assert.strictEqual(new Buffer(10).utf8Write('½½½'), 6);
    });
    it('length is zero', function() {
        assert.strictEqual(new Buffer(0).utf8Write.length, 0);
    });
});

describe('Buffer.utf8Slice', function() {
    it('should convert arguments', function() {
        assert.strictEqual(new Buffer(10).fill(0).utf8Slice(false, true), '\u0000');
    });
    it('should check range', function() {
        assert.throws(() => {
            new Buffer(10).utf8Slice(-1, 10)
        }, RangeError);
    });
    it('should check range #2', function() {
        assert.throws(() => {
            new Buffer(10).utf8Slice(11, 10)
        }, RangeError);
    });
    it('should check range #3', function() {
        assert.throws(() => {
            new Buffer(10).utf8Slice(0, -1)
        }, RangeError);
    });
    it('should check range #4', function() {
        assert.throws(() => {
            new Buffer(10).utf8Slice(0, 11)
        }, RangeError);
    });
    it('should slice the full buffer with no arguments', function() {
        assert.strictEqual(new Buffer(10).fill(0).utf8Slice().length, 10);
    });
    it('should slice using the proper default value', function() {
        assert.strictEqual(new Buffer(10).fill(0).utf8Slice(5).length, 5);
    });
    it('should return an empty string', function() {
        assert.strictEqual(new Buffer(0).utf8Slice(-5, -10), '');
    });
    it('should return an empty string #2', function() {
        assert.strictEqual(new Buffer(0).utf8Slice(20, 10), '');
    });
    it('should return an empty string #3', function() {
        assert.strictEqual(new Buffer(0).utf8Slice('hi', 'there'), '');
    });
    it('should return an empty string #4', function() {
        assert.strictEqual(new Buffer(0).utf8Slice(true, false), '');
    });
    it('should check buffer type', function() {
        assert.throws(() => {
            Buffer.prototype.utf8Slice.call(1)
        }, TypeError);
    });
    it('length is zero', function() {
        assert.strictEqual(new Buffer(0).utf8Slice.length, 0);
    });
});