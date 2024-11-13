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
var child_process = require('child_process');
var module = require('./_unit');
var spawnSync = child_process.spawnSync;

function checkTheAnswerToLifeTheUniverseAndEverything(answer) {
    assert.strictEqual(answer.stderr.toString(), '');
    assert.strictEqual(answer.stdout.toString(), '42\n');
    assert.strictEqual(answer.status, 0);
}

describe('Spawn', function () {
    this.timeout(40000);
    it('should spawn a child node process when env. variables are cleared', function () {
        var result = spawnSync(process.execPath, ['-p', '6*7'], {env: {}});
        checkTheAnswerToLifeTheUniverseAndEverything(result);
    });
    it('should accept max_old_space_size option', function () {
        var result = spawnSync(process.execPath, ['--max_old_space_size=1024', '-p', '6*7']);
        checkTheAnswerToLifeTheUniverseAndEverything(result);
    });
    it('should accept --stack-trace-limit option', function () {
        var result = spawnSync(process.execPath, ['--stack-trace-limit=42', '-p', 'Error.stackTraceLimit']);
        checkTheAnswerToLifeTheUniverseAndEverything(result);
    });
    it('should accept --stack-trace-limit option in NODE_OPTIONS', function () {
        var result = spawnSync(process.execPath, ['-p', 'Error.stackTraceLimit'], {env: { NODE_OPTIONS: '--stack-trace-limit=42' }});
        checkTheAnswerToLifeTheUniverseAndEverything(result);
    });
    it('should accept --use-strict option', function () {
        var result = spawnSync(process.execPath, ['--use-strict', '-p', '6*7']);
        checkTheAnswerToLifeTheUniverseAndEverything(result);
    });
    it('should survive duplicates in envPairs', function (done) {
        // Copy the current content of process.env
        var envPairs = [];
        for (var key in process.env) {
            envPairs.push(key + '=' + process.env[key]);
        }

        // Add duplicates
        envPairs.push('MY_VAR=1');
        envPairs.push('MY_VAR=2');

        // Spawn a new process with these envPairs and try to list keys of process.env
        var myProcess = new child_process.ChildProcess();
        myProcess.on('exit', function (code) {
            assert.strictEqual(code, 0);
            done();
        });
        myProcess.spawn({
            file: process.execPath,
            args: ['node', '-e', 'Object.keys(process.env)'],
            stdio: 'inherit',
            envPairs: envPairs
        });
    });
    ['--help', '-h', '--v8-options'].forEach(function (option) {
        it('should print help when ' + option + ' option is used', function () {
            var result = spawnSync(process.execPath, [option]);
            assert.strictEqual(result.status, 0);
            assert.match(result.stdout.toString(), /Options:/);
        });
    });
    if (module.hasJavaInterop()) {
        it('should finish gracefully when a native method is called from a wrong thread', function () {
            var code = `var vm = require('vm');
                        var ctx = org.graalvm.polyglot.Context.newBuilder("js").allowAllAccess(true).build();
                        ctx.eval("js", "java.lang.Thread.setDefaultUncaughtExceptionHandler((t, e) => {e.printStackTrace(); java.lang.System.exit(1);});");
                        var sandbox = {};
                        vm.runInNewContext("var f = function() { console.log('crash'); }; var t = new java.lang.Thread(f);", sandbox);
                        var t = sandbox.t;
                        t.start();
                        t.join();`;
            code = code.replace(/\n\s*/g, ' ');
            var result = spawnSync(process.execPath, ['-e', code]);
            assert.ok(result.stderr.toString().indexOf('thread') !== -1);
            assert.strictEqual(result.stdout.toString(), '');
            assert.strictEqual(result.status, 1);
        });
    }
    it('should finish gracefully when the process is terminated from an inner context', function () {
        var code = `require('vm').runInNewContext('process.exit()', { process: process })`;
        var result = spawnSync(process.execPath, ['-e', code]);
        assert.strictEqual(result.stdout.toString(), '');
        assert.strictEqual(result.stderr.toString(), '');
        assert.strictEqual(result.status, 0);
    });
    if (process.platform === 'linux') {
        // The warning that we check was printed into stdout! It was printed
        // between fork and execvp. So, it was wiped when stdout was block-buffered.
        // => using stdbuf -oL to force line buffering (which is the default
        // in terminal when the output is not redirected)
        var stdbuf = spawnSync('which', ['stdbuf']);
        if (stdbuf.stdout.toString().trim() !== '') { // stdbuf exists
            it('should not print a warning when spawning a process with inherit stdio', function () {
                this.timeout(40000);
                var result = spawnSync('stdbuf', ['-oL', process.execPath, '-e', 'child_process.spawnSync(process.execPath, ["-p", "6*7"], { stdio: "inherit"})']);
                checkTheAnswerToLifeTheUniverseAndEverything(result);
            });
        }
    }
    it('should finish with exit code and not print stack trace of exit exception (process.exit())', function() {
            var result = spawnSync(process.execPath, ['-e', 'process.exit(42); console.error("should not reach here");']);
            assert.strictEqual(result.stderr.toString(), '');
            assert.strictEqual(result.stdout.toString(), '');
            assert.strictEqual(result.status, 42);
    });
    it.skipOnNode('should finish with exit code and not print stack trace of exit exception (quit())', function() {
            var result = spawnSync(process.execPath, ['--experimental-options', '--js.shell', '-e', 'quit(42); console.error("should not reach here");']);
            assert.strictEqual(result.stderr.toString(), '');
            assert.strictEqual(result.stdout.toString(), '');
            assert.strictEqual(result.status, 42);
    });
    it('should not throw when FormData are accessed', function() {
            var result = spawnSync(process.execPath, ['-e', 'typeof FormData']);
            assert.strictEqual(result.stderr.toString(), '');
            assert.strictEqual(result.stdout.toString(), '');
            assert.strictEqual(result.status, 0);
    });
    it('should accept creation of identical synthetic modules', function () {
        var moduleCreation = 'new vm.SyntheticModule([], () => {}, { identifier: "foo" }).link(() => {});';
        var code = moduleCreation + moduleCreation;
        var result = spawnSync(process.execPath, [
            '--experimental-vm-modules',
            '--no-warnings=ExperimentalWarning',
            '-e', code],
            { env: { ...process.env, NODE_JVM_OPTIONS: (process.env.NODE_JVM_OPTIONS || '') + ' -ea' }});
        assert.strictEqual(result.stderr.toString(), '');
        assert.strictEqual(result.stdout.toString(), '');
        assert.strictEqual(result.status, 0);
    });
});
