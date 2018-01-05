/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Undefined', function () {
    describe('IsUndefined', function () {
        it('should return true', function () {
            assert.strictEqual(module.Undefined_IsUndefined(), true);
        });
    });
    describe('IsNull', function () {
        it('should return false', function () {
            assert.strictEqual(module.Undefined_IsNull(), false);
        });
    });
    describe('IsTrue', function () {
        it('should return false', function () {
            assert.strictEqual(module.Undefined_IsTrue(), false);
        });
    });
    describe('IsFalse', function () {
        it('should return false', function () {
            assert.strictEqual(module.Undefined_IsFalse(), false);
        });
    });
    describe('IsNativeError', function () {
        it('should return false', function () {
            assert.strictEqual(module.Value_IsNativeError(undefined), false);
        });
    });
});
