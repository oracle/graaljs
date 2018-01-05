/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Local', function () {
    describe('IsEmpty', function () {
        it('should return false for string', function () {
            var s = "foo";
            assert.strictEqual(module.Local_IsEmpty(s), false);
        });
        it('should return true after clear', function () {
            var s = "foo";
            assert.strictEqual(module.Local_IsEmptyAfterClear(s), true);
        });
    });
    describe('Operator ==', function () {
        it('should return true for same string', function () {
            var s = "foo";
            var t = s;
            assert.strictEqual(module.Local_OperatorEquals(s, t), true);
        });
        it('should return true for same object', function () {
            var s = {a: "foo", b: 12};
            var t = s;
            assert.strictEqual(module.Local_OperatorEquals(s, t), true);
        });
        it('should return true for identical strings', function () {
            var s = "foo";
            var t = "foo";
            assert.strictEqual(module.Local_OperatorEquals(s, t), true);
        });
        it('should return false for different strings', function () {
            var s = "foo";
            var t = "bar";
            assert.strictEqual(module.Local_OperatorEquals(s, t), false);
        });
        it('should return false for identical object', function () {
            var s = {a: "foo", b: 12};
            var t = {a: "foo", b: 12};
            assert.strictEqual(module.Local_OperatorEquals(s, t), false);
        });
    });
    describe('Operator !=', function () {
        it('should return false for same string', function () {
            var s = "foo";
            var t = s;
            assert.strictEqual(module.Local_OperatorNotEquals(s, t), false);
        });
        it('should return false for same object', function () {
            var s = {a: "foo", b: 12};
            var t = s;
            assert.strictEqual(module.Local_OperatorNotEquals(s, t), false);
        });
        it('should return false for identical strings', function () {
            var s = "foo";
            var t = "foo";
            assert.strictEqual(module.Local_OperatorNotEquals(s, t), false);
        });
        it('should return true for different strings', function () {
            var s = "foo";
            var t = "bar";
            assert.strictEqual(module.Local_OperatorNotEquals(s, t), true);
        });
        it('should return true for identical object', function () {
            var s = {a: "foo", b: 12};
            var t = {a: "foo", b: 12};
            assert.strictEqual(module.Local_OperatorNotEquals(s, t), true);
        });
    });
});
