/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Message', function () {
    var throwException = function () {
        throw "exception";
    };
    describe('GetStartColumn', function () {
        it('querying simple message', function () {
            assert.strictEqual(module.Message_GetStartColumn(throwException) > 0, true);
        });
    });
    describe('GetEndColumn', function () {
        it('querying simple message', function () {
            assert.strictEqual(module.Message_GetEndColumn(throwException) > 0, true);
        });
    });
    describe('GetLineNumber', function () {
        it('querying simple message', function () {
            assert.strictEqual(module.Message_GetLineNumber(throwException) > 0, true);
        });
    });
    describe('GetSourceLine', function () {
        it('querying simple message', function () {
            var line = module.Message_GetSourceLine(throwException);
            assert.strictEqual(line.length > 0, true);
            assert.strictEqual(typeof line, "string");
        });
    });
    describe('GetScriptResourceName', function () {
        it('querying simple message', function () {
            var line = module.Message_GetScriptResourceName(throwException);
            assert.strictEqual(line.length > 0, true);
            assert.strictEqual(typeof line, "string");
        });
    });
});
