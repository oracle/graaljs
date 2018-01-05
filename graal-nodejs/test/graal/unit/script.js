/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

var ret42Script = "function myF() { return 42; }; myF();";

describe('Script', function () {
    describe('Compile', function () {
        it('should be able to compile an easy script', function () {
            var scriptId = module.Script_Compile(ret42Script, "");
            assert.strictEqual(scriptId > 0, true);
        });
        it('should provide consecutive script Ids', function () {
            //this is not actually a hard requirement, it is a sanity test
            var scriptId1 = module.Script_Compile(ret42Script, "file1.js");
            assert.strictEqual(scriptId1 > 0, true);
            var scriptId2 = module.Script_Compile(ret42Script, "file2.js");
            assert.strictEqual(scriptId2 > 0, true);
            assert.strictEqual(scriptId1 + 1, scriptId2);
        });
        it('should also work when providing a ScriptOrigin', function () {
            var scriptId = module.Script_CompileWithScriptOrigin(ret42Script, "");
            assert.strictEqual(scriptId > 0, true);
        });
    });
    describe('Run', function () {
        it('should be able to run an easy script', function () {
            var result = module.Script_Run(ret42Script, "");
            assert.strictEqual(result, 42);
        });
    });
});
