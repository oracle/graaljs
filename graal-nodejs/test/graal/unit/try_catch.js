/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('TryCatch', function () {
    describe('HasCaught', function () {
        it('should return false when no exception occured', function () {
            assert.strictEqual(module.TryCatch_HasCaughtNoException(), false);
        });
        it('should return true when exception is thrown by native code', function () {
            assert.strictEqual(module.TryCatch_HasCaughtNativeException(), true);
        });
        it('should return true when exception is thrown by JavaScript code', function () {
            assert.strictEqual(module.TryCatch_HasCaughtJSException(function () {
                throw "exception thrown by the JavaScript callback";
            }), true);
        });
        it('should work with nested TryCatch blocks (outer)', function () {
            assert.strictEqual(module.TryCatch_HasCaughtNestedOuter(function () {
                throw "exception";
            }), true);
        });
        it('should work with nested TryCatch blocks (inner)', function () {
            assert.strictEqual(module.TryCatch_HasCaughtNestedInner(function () {
                throw "exception";
            }), true);
        });
        it('should work with nested TryCatch blocks (both)', function () {
            assert.strictEqual(module.TryCatch_HasCaughtNestedBoth(function () {
                throw "exception";
            }), true);
        });
    });
    describe('Exception', function () {
        it('should return an empty handle when no exception occured', function () {
            assert.strictEqual(module.TryCatch_ExceptionForNoExceptionIsEmpty(), true);
        });
        it('should return the exception that occurred', function () {
            var exception = new Error("test exception");
            assert.strictEqual(module.TryCatch_ExceptionForThrownException(function () {
                throw exception;
            }), exception);
        });
    });
    describe('ReThrow', function () {
        it('should rethrow an exception', function () {
            var thrownException = new Error("test exception");
            var caughtException;
            try {
                module.TryCatch_ReThrowException(function () {
                    throw thrownException;
                });
            } catch (err) {
                caughtException = err;
            }
            assert.strictEqual(caughtException, thrownException);
        });
        it('should rethrow an exception through several nested try-catch blocks', function () {
            var exception = new Error("test exception");
            assert.strictEqual(module.TryCatch_ReThrowNested(function () {
                throw exception;
            }), exception);
        });
    });
    describe('SetVerbose', function () {
        it('call SetVerbose(true)', function () {
            assert.strictEqual(module.TryCatch_SetVerbose(true), true);
        });
        it('call SetVerbose(false)', function () {
            assert.strictEqual(module.TryCatch_SetVerbose(false), true);
        });
    });
    describe('Message', function () {
        it('can access SourceLine of the Message', function () {
            var line = module.TryCatch_MessageGetSourceLine(function () {
                throw "exception";
            });
            assert.strictEqual(line.length > 0, true);
            assert.strictEqual(typeof line, "string");
        });
    });
    describe.skip('HasTerminated', function () {
        it('should be callable', function () {
            module.TryCatch_HasTerminatedNoException();
            assert.strictEqual(true, true);
        });
    });
});
