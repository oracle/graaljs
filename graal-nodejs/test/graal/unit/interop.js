/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
var repl = require('repl');
var vm = require('vm');

describe('Polyglot', function () {
    if (typeof Polyglot === 'object') {
        it('eval should work in a new context', function () {
            assert.strictEqual(require('vm').runInNewContext("Polyglot.eval('application/javascript', '7*6')"), 42);
        });
        it('export/import should work in a new context', function () {
            assert.strictEqual(require('vm').runInNewContext("Polyglot.export('obj', { foo: 'bar' }); Polyglot.import('obj').foo"), 'bar');
        });
    }
    if (module.hasJavaInterop()) {
        it('auto-completion in REPL should work for foreign objects', function () {
            this.timeout(10000);
            var theREPL = repl.start('');
            try {
                theREPL.complete('java.lang.System.out.pr', function (error, data) {
                    assert.ifError(error);
                    assert.deepEqual(data, [
                        [
                            'java.lang.System.out.propertyIsEnumerable',
                            '',
                            'java.lang.System.out.print',
                            'java.lang.System.out.printf',
                            'java.lang.System.out.println'
                        ],
                        'java.lang.System.out.pr'
                    ]);
                });
            } finally {
                theREPL.close();
            }
        });
    }
});
