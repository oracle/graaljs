/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('GC', function () {
    it('callbacks registered by AddGCPrologueCallback should be triggered by gc()', function () {
        assert.strictEqual(module.GC_AddGCPrologueCallbackTest(), true);
    });
    it('callbacks registered by AddGCEpilogueCallback should be triggered by gc()', function () {
        assert.strictEqual(module.GC_AddGCEpilogueCallbackTest(), true);
    });
    it('callback registered as both prologue and epilogue should be invoked twice by gc()', function () {
        assert.strictEqual(module.GC_AddDoubleGCCallbackTest(), true);
    });
    it('RemoveGCPrologueCallback should remove a registered callback', function () {
        assert.strictEqual(module.GC_RemoveGCPrologueCallbackTest(), true);
    });
    it('RemoveGCEpilogueCallbackTest should remove a registered callback', function () {
        assert.strictEqual(module.GC_RemoveGCEpilogueCallbackTest(), true);
    });
});
