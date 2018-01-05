/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

// Add skipOnGraal verb to the default bdd UI
var bddUI = Mocha.interfaces['bdd'];
Mocha.interfaces['graal'] = function (suite) {
    bddUI(suite);
    suite.on('pre-require', function (context) {
        context.it.skipOnGraal = function (title, fn) {
            if (isGraalJS()) {
                context.it.skip(title, fn);
            } else {
                return context.it(title, fn);
            }
        };
        context.describe.skipOnGraal = function (title, fn) {
            if (isGraalJS()) {
                context.describe.skip(title, fn);
            } else {
                return context.describe(title, fn);
            }
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
