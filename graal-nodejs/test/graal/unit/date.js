/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

describe('Date', function () {
    describe('toString()', function () {
        it('should pick up changes of process.env.TZ', function () {
            const date = new Date('2018-04-14T12:34:56.789Z');
            const initialDateString = date.toString();
            const initialTZ = process.env.TZ;

            process.env.TZ = 'Europe/Amsterdam';
            assert.strictEqual(
                date.toString().replace('Central European Summer Time', 'CEST'),
                'Sat Apr 14 2018 14:34:56 GMT+0200 (CEST)');

            process.env.TZ = 'Etc/UTC';
            assert.strictEqual(
                date.toString().replace('Coordinated Universal Time', 'UTC').replace('(GMT)', '(UTC)'),
                'Sat Apr 14 2018 12:34:56 GMT+0000 (UTC)');

            process.env.TZ = 'Asia/Tokyo';
            assert.strictEqual(
                date.toString().replace('Japan Standard Time', 'JST'),
                'Sat Apr 14 2018 21:34:56 GMT+0900 (JST)');

            if (initialTZ === undefined) {
                delete process.env.TZ;
            } else {
                process.env.TZ = initialTZ;
            }
            assert.strictEqual(
                date.toString(),
                initialDateString);
        });
    });
});
