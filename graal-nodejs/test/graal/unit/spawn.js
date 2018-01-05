/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var spawnSync = require('child_process').spawnSync;

describe('Spawn', function () {
    it('should spawn a child node process when env. variables are cleared', function () {
        var result = spawnSync(process.execPath, ['-p', '6*7'], {env: {}});
        assert.strictEqual(result.stderr.toString(), '');
        assert.strictEqual(result.stdout.toString(), '42\n');
        assert.strictEqual(result.status, 0);
    }).timeout(10000);
    if (typeof java === 'object') {
        it('should finish gracefully when a native method is called from a wrong thread', function () {
            var code = "var t = new java.lang.Thread(function() { console.log('crash'); }); t.start(); t.join()";
            var result = spawnSync(process.execPath, ['-e', code]);
            assert.ok(result.stderr.toString().indexOf('thread') !== 0);
            assert.strictEqual(result.stdout.toString(), '');
            assert.strictEqual(result.status, 1);
        }).timeout(10000);
    }
});
