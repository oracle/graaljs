/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Context', function () {
    describe('GetGlobal', function () {
        it('get global object', function () {
            var global = module.Context_Global();

            assert.strictEqual(global instanceof Object, true);
            assert.strictEqual(Object.keys(global).length > 20, true);
            assert.strictEqual(global.hasOwnProperty("Array"), true);
            assert.strictEqual(global["Array"] === Array, true);
        });
    });
    describe('Enter / Exit', function () {
        it('enter and exit a new Context', function () {
            assert.strictEqual(module.Context_EnterAndExitNewContext(), true);
        });
    });
    describe('New', function () {
        it('can create a global object with an additional property ', function () {
            assert.strictEqual(module.Context_New(), true);
        });
    });
    describe('SecurityTokens', function () {
        it('get security token', function () {
            var token = module.Context_GetSecurityToken();
            assert.strictEqual(token instanceof Object, true);
        });
        it('set the same security token again', function () {
            var token1 = module.Context_GetSecurityToken();
            assert.strictEqual(token1 instanceof Object, true);
            module.Context_SetSecurityToken(token1);
            var token2 = module.Context_GetSecurityToken();
            assert.strictEqual(token1 === token2, true);
        });
        it('call UseDefaultSecurityToken', function () {
            assert.strictEqual(module.Context_UseDefaultSecurityToken(), true);
        });
    });
});
