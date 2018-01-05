/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

// Tests basic functionality that other tests depend on (like whether
// false/true/undefined/null passed from the native code is really
// false/true/undefined/null when it gets to JavaScript.
describe('Bootstrap', function () {
    it('should return undefined', function () {
        assert.strictEqual(module.Bootstrap_Undefined(), undefined);
    });
    it('should return null', function () {
        assert.strictEqual(module.Bootstrap_Null(), null);
    });
    it('should return true', function () {
        assert.strictEqual(module.Bootstrap_True(), true);
    });
    it('should return false', function () {
        assert.strictEqual(module.Bootstrap_False(), false);
    });
    it('should return int32', function () {
        assert.strictEqual(module.Bootstrap_Int32(), 211);
    });
    it('should return uint32', function () {
        assert.strictEqual(module.Bootstrap_Uint32(), 3000000000);
    });
    it('should return double', function () {
        assert.strictEqual(module.Bootstrap_Double(), 3.14);
    });
    describe('ReturnValue::Get()', function() {
        it('should return undefined when no value was set', function() {
            assert.strictEqual(module.Bootstrap_GetNotSet(), undefined);
        });
        it('should work for int32 values', function() {
            assert.strictEqual(module.Bootstrap_GetInt32(), true);
        });
        it('should work for uint32 values', function() {
            assert.strictEqual(module.Bootstrap_GetUint32(), true);
        });
        it('should work for double values', function() {
            assert.strictEqual(module.Bootstrap_GetDouble(), true);
        });
    });
});
