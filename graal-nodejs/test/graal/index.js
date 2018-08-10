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

var Mocha = require('mocha');
var fs = require('fs');

var limitToSuites = [];
for (var i = 2; i < process.argv.length; i++) {
    var arg = process.argv[i];
    if (arg.length > 0) {
        suite = process.argv[i];
        console.log("limiting to suite:", suite);
        limitToSuites.push(suite);
    }
}

var isGraalJS = function () {
    // TODO: replace by a more reliable check
    return typeof (print) === 'function';
};

// Add skipOnGraal, skipOnNode verbs to the default bdd UI
var bddUI = Mocha.interfaces['bdd'];
Mocha.interfaces['graal'] = function (suite) {
    bddUI(suite);
    suite.on('pre-require', function (context) {
        var describeSkip = function(shouldSkip, title, fn) {
            if (shouldSkip()) {
                context.describe.skip(title, fn);
            } else {
                return context.describe(title, fn);
            }
        };
        var itSkip = function(shouldSkip, title, fn) {
            if (shouldSkip()) {
                context.it.skip(title, fn);
            } else {
                return context.it(title, fn);
            }            
        };
        context.it.skipOnGraal = function (title, fn) {
            return itSkip(isGraalJS, title, fn);
        };
        context.describe.skipOnGraal = function (title, fn) {
            return describeSkip(isGraalJS, title, fn);
        };
        context.it.skipOnNode = function (title, fn) {
            return itSkip(() => !isGraalJS(), title, fn);
        };
        context.describe.skipOnNode = function (title, fn) {
            return describeSkip(() => !isGraalJS(), title, fn);
        };
    });
};

var mocha = new Mocha().ui('graal');

var files = fs.readdirSync('unit');
var filesLength = files.length;
for (var i = 0; i < filesLength; i++) {
    var file = files[i];
    if (file.indexOf('.js', file.length - 3) !== -1) { // file.endsWith('.js')
        if (limitToSuites.length === 0 || limitToSuites.indexOf(file) >= 0) {
            mocha.addFile('unit/' + file);
        }
    }
}

mocha.run(function (failures) {
    process.on('exit', function () {
        process.exit(failures);  // exit with non-zero status if there were failures
    });
});
