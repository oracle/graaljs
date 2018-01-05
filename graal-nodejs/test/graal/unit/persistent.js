/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Persistent', function () {
    describe('New', function () {
        it('should create a persistent object', function () {
            var obj = {a: 123};
            var pers = module.Persistent_New(obj);
            assert.strictEqual(pers, obj);
        });
    });
    describe('Reset', function () {
        it('should be able to call Reset', function () {
            var obj = {a: 123};
            var disp = module.Persistent_Reset(obj);
            assert.strictEqual(disp, true);
        });
    });
});
