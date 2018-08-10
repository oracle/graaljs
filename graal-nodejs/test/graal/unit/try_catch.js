/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

var assert = require('assert');
var module = require('./_unit');
var URL = require('url').URL;

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
    describe('Fatal Error', function () {
        it('should not be triggered when an error is thrown from a promise job (part 1)', function (done) {
            new Promise(function(resolve) {
              setTimeout(() => resolve(), 1000);
            }).then(function() {
              try {
                new URL('[invalid]');
              } catch (e) {
                done();
              }
            });
        });
        it('should not be triggered when an error is thrown from a promise job (part 2)', function (done) {
            new Promise(function(resolve) {
              setTimeout(() => resolve(), 1000);
            }).then(function() {
                new URL('[invalid]');
            }).catch(function(e) {
                assert.strictEqual(e instanceof TypeError, true);
                done();
            });
        });
    });
});
