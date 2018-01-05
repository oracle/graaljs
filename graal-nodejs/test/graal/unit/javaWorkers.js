/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Java interop threads', function() {
    if (typeof Java !== 'object') {
        // no interop
        return;
    }
    it('can create and kill a worker', function(done) {
        var worker = new Java.Worker();
        worker.terminate();
        done();
    });
    it('can start, consume result, and complete', function (done) {
        // Original Node.js 6.x does not support async function
        // => keeping the code of this test in string literal so that
        // the unit tests do not fail with syntax error on the original Node.js
        var code =
                "(async function foo() {" +
                "    var method = Java.type('java.lang.Thread').currentThread;" +
                "    var worker = new Java.Worker();" +
                "    var result = await worker.submit(method);" +
                "    assert(result.getName().contains('thread'));" +
                "    worker.terminate();" +
                "    done();" +
                "})();";
        eval(code);
    });
});