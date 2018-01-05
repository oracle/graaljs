/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('V8', function () {
    describe('Initialize', function () {
        it('should return true', function () {
            assert.strictEqual(module.V8Obj_Initialize(), true);
        });
    });
    describe.skip('Dispose', function () {
        it('should shut down V8', function () {
            assert.strictEqual(module.V8Obj_Dispose(), true);
        });
    });
    describe('SetEntropySource', function () {
        it('should be possible to set entropy source callback', function () {
            assert.strictEqual(module.V8Obj_SetEntropySource(), true);
        });
    });
    describe('SetFlagsFromCommandLine', function () {
        it('should be possible to call SetFlagsFromCommandLine', function () {
            assert.strictEqual(module.V8Obj_SetFlagsFromCommandLine(), true);
        });
    });
    describe('SetFlagsFromString', function () {
        it('setting simple flag', function () {
            assert.strictEqual(module.V8Obj_SetFlagsFromString(), true);
        });
    });
    describe('GetVersion', function () {
        it('can read a version string', function () {
            var version = module.V8Obj_GetVersion();
            assert.strictEqual(typeof version, "string");
            assert.strictEqual(version.length > 0, true);
        });
    });
});

describe('ResourceConstraints', function () {
    describe('ResourceConstraints constructor', function () {
        it('should be able to create and get/set values', function () {
            assert.strictEqual(module.V8Obj_ResourceConstraints(), true);
        });
    });
});
