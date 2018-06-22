#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------

import os, zipfile, re, shutil, tarfile
from os.path import join, exists, isdir, getmtime

import mx_graal_js_benchmark # pylint: disable=unused-import
import mx, mx_sdk
from mx_gate import Task, add_gate_runner
from mx_unittest import unittest

_suite = mx.suite('graal-js')

class GraalJsDefaultTags:
    default = 'default'
    all = 'all'

def _graal_js_gate_runner(args, tasks):
    with Task('TestJSCommand', tasks, tags=[GraalJsDefaultTags.default, GraalJsDefaultTags.all]) as t:
        if t:
            js(['-Dtruffle.js.ProfileTime=true', '-e', '""'])

    with Task('UnitTests', tasks, tags=[GraalJsDefaultTags.default, GraalJsDefaultTags.all]) as t:
        if t:
            unittest(['-Dtruffle.js.NashornJavaInterop=true', '--enable-timing', '--very-verbose', 'com.oracle.truffle.js.scriptengine.test'])

    gateTestConfigs = {
        GraalJsDefaultTags.default: ['gate'],
        'noic': ['gate', '-Dtruffle.js.PropertyCacheLimit=0', '-Dtruffle.js.FunctionCacheLimit=0'],
        'directbytebuffer': ['gate', '-Dtruffle.js.DirectByteBuffer=true'],
        'cloneuninitialized': ['gate', '-Dtruffle.js.TestCloneUninitialized=true'],
        'lazytranslation': ['gate', '-Dtruffle.js.LazyTranslation=true'],
    }

    gateTestCommands = {
        'Test262': test262,
        'TestNashorn': testnashorn,
        'TestV8': testv8,
        'TestInstrumentation': testinstrumentation,
    }

    for testCommandName in gateTestCommands:
        for testConfigName in gateTestConfigs:
            testName = '%s-%s' % (testCommandName, testConfigName)
            with Task(testName, tasks, tags=[testName, testConfigName, GraalJsDefaultTags.all]) as t:
                if t:
                    gateTestCommands[testCommandName](gateTestConfigs[testConfigName])

add_gate_runner(_suite, _graal_js_gate_runner)

class ArchiveProject(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.ArchivableProject.__init__(self, suite, name, deps, workingSets, theLicense)
        assert 'prefix' in args
        assert 'outputDir' in args

    def output_dir(self):
        return join(self.dir, self.outputDir)

    def archive_prefix(self):
        return self.prefix

    def getResults(self):
        return mx.ArchivableProject.walk(self.output_dir())

class Icu4jDataProject(ArchiveProject):
    def getBuildTask(self, args):
        return Icu4jBuildTask(self, args, 1)

    def getResults(self):
        return ArchiveProject.getResults(self)

class Icu4jBuildTask(mx.ArchivableBuildTask):
    def __init__(self, *args):
        mx.ArchivableBuildTask.__init__(self, *args)
        self.icuDir = join(_suite.dir, 'lib', 'icu4j')

    def needsBuild(self, newestInput):
        if not exists(self.icuDir):
            return (True, self.icuDir + " not found")
        icu4jDep = mx.dependency('ICU4J')
        icu4jPath = icu4jDep.get_path(resolve=True)
        if getmtime(icu4jPath) > getmtime(self.icuDir):
            return (True, self.icuDir + " is older than " + icu4jPath)
        return (False, None)

    def build(self):
        unpackIcuData([])

    def clean(self, forBuild=False):
        if exists(self.icuDir):
            mx.rmtree(self.icuDir)

def unpackIcuData(args):
    """populate ICU4J localization data from jar file dependency"""

    icu4jDataDir = join(_suite.dir, 'lib', 'icu4j')
    # clean up first
    if isdir(icu4jDataDir):
        shutil.rmtree(icu4jDataDir)
    icu4jPackageDir = 'com/ibm/icu/impl/data'
    # unpack the files
    icu4jDep = mx.dependency('ICU4J')
    icu4jPath = icu4jDep.get_path(resolve=True)
    mx.log("ICU4J dependency found in %s" % (icu4jPath))
    with zipfile.ZipFile(icu4jPath, 'r') as zf:
        toExtract = [e for e in zf.namelist() if e.startswith(icu4jPackageDir) and not e.endswith(".class") and not e.endswith(".html")]
        zf.extractall(icu4jDataDir, toExtract)
        mx.log("%d files extracted to %s" % (len(toExtract), icu4jDataDir))
    # move the stuff such that the path is stable
    for f in os.listdir(join(icu4jDataDir, icu4jPackageDir)):
        if re.match('icudt.*', f):
            icu4jUnzippedDataPath = join(icu4jDataDir, icu4jPackageDir, f)
            icu4jDataPath = join(icu4jDataDir, "icudt")
            shutil.move(icu4jUnzippedDataPath, icu4jDataPath)
            shutil.rmtree(join(icu4jDataDir, "com"))
            mx.log('Use the following parameters when invoking svm version of js to make ICU4J localization data available for the runtime:\n-Dpolyglot.js.intl-402=true -Dcom.ibm.icu.impl.ICUBinary.dataPath=%s' % icu4jDataPath)

def parse_js_args(args, default_cp=None, useDoubleDash=False):
    vm_args, remainder, cp = [], [], []
    if default_cp is None:
        default_cp = []
    skip = False
    for (i, arg) in enumerate(args):
        if skip:
            skip = False
            continue
        elif any(arg.startswith(prefix) for prefix in ['-X', '-G:', '-D', '-verbose', '-ea', '-javaagent']) or arg in ['-esa', '-d64']:
            vm_args += [arg]
        elif useDoubleDash and arg == '--':
            remainder += args[i:]
            break
        elif arg in ['-cp', '-classpath']:
            if i + 1 < len(args):
                cp = [args[i + 1]] # Last one wins
                skip = True
            else:
                mx.abort('{} must be followed by a classpath'.format(arg))
        else:
            remainder += [arg]
    cp = default_cp + cp
    if cp:
        vm_args = ['-cp', ':'.join(cp)] + vm_args
    return vm_args, remainder

def _default_stacksize():
    if mx.get_arch() is 'sparcv9':
        return '24m'
    return '16m'

def _append_default_js_vm_args(vm_args, min_heap='2g', max_heap='2g', stack_size=_default_stacksize()):
    if not any(x.startswith('-Xm') for x in vm_args):
        if min_heap:
            vm_args += ['-Xms' + min_heap]
        if max_heap:
            vm_args += ['-Xmx' + max_heap]
    if stack_size and not any(x.startswith('-Xss') for x in vm_args):
        vm_args += ['-Xss' + stack_size]
    return vm_args

def _js_cmd_line(args, main_class, default_cp=None, append_default_args=True):
    _vm_args, _js_args = parse_js_args(args, default_cp=default_cp)
    if append_default_args:
        _vm_args = _append_default_js_vm_args(_vm_args)
    return _vm_args + [main_class] + _js_args

def graaljs_cmd_line(args, append_default_args=True):
    return _js_cmd_line(args + ['-Dtruffle.js.BindProgramResult=false'], main_class=mx.distribution('GRAALJS_LAUNCHER').mainClass, default_cp=[mx.classpath(['tools:CHROMEINSPECTOR', 'tools:TRUFFLE_PROFILER', 'GRAALJS_LAUNCHER', 'GRAALJS'])], append_default_args=append_default_args)

def js(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    """Run the REPL or a JavaScript program with Graal.js"""
    return mx.run_java(graaljs_cmd_line(args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def nashorn(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    """Run the REPL or a JavaScript program with Nashorn"""
    return mx.run_java(_js_cmd_line(args, main_class='jdk.nashorn.tools.Shell'), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def _fetch_test_suite(dest, library_names):
    def _get_lib_path(_lib_name):
        return mx.library(_lib_name).get_path(resolve=True)

    _extract = False
    for _lib_name in library_names:
        if not exists(dest) or getmtime(_get_lib_path(_lib_name)) > getmtime(dest):
            mx.logv('{} needs to be extracted'.format(_lib_name))
            _extract = True
            break

    if _extract:
        if exists(dest):
            mx.logv('Deleting the old test directory {}'.format(dest))
            shutil.rmtree(dest)
            mx.ensure_dir_exists(dest)
        for _lib_name in library_names:
            with tarfile.open(_get_lib_path(_lib_name), 'r') as _tar:
                _tar.extractall(dest)

def _run_test_suite(location, library_names, custom_args, default_vm_args, max_heap, stack_size, main_class, nonZeroIsFatal, cwd):
    _fetch_test_suite(location, library_names)
    _vm_args, _prog_args = parse_js_args(custom_args)
    _vm_args = _append_default_js_vm_args(vm_args=_vm_args, max_heap=max_heap, stack_size=stack_size)
    _vm_args = ['-ea', '-esa', '-cp', mx.classpath('TRUFFLE_JS_TESTS')] + default_vm_args + _vm_args
    return mx.run_java(_vm_args + [main_class] + _prog_args, nonZeroIsFatal=nonZeroIsFatal, cwd=cwd)

def test262(args, nonZeroIsFatal=True):
    """run the test262 conformance suite"""
    _location = join(_suite.dir, 'lib', 'test262')
    _default_vm_args = [
        '-Dtruffle.js.NashornJavaInterop=false',
        '-Dtruffle.js.Test262Mode=true',
        '-Dtruffle.js.SIMDJS=true',
    ]
    return _run_test_suite(
        location=_location,
        library_names=['TEST262'],
        custom_args=args,
        default_vm_args=_default_vm_args,
        max_heap='4g',
        stack_size='1m',
        main_class='com.oracle.truffle.js.test.external.test262.Test262',
        nonZeroIsFatal=nonZeroIsFatal,
        cwd=_suite.dir
    )

def testnashorn(args, nonZeroIsFatal=True):
    """run the testNashorn conformance suite"""
    _location = join(_suite.dir, 'lib', 'testnashorn')
    _default_vm_args = [
        '-Dtruffle.js.NashornCompatibilityMode=true',
        '-Dtruffle.js.U180EWhitespace=true',
        '-Dtruffle.js.NashornJavaInterop=true',
    ]
    _stack_size = '2m' if mx.get_arch() is 'sparcv9' else '1m'
    _run_test_suite(
        location=_location,
        library_names=['TESTNASHORN', 'TESTNASHORN_EXTERNAL'],
        custom_args=args,
        default_vm_args=_default_vm_args,
        max_heap='2g',
        stack_size=_stack_size,
        main_class='com.oracle.truffle.js.test.external.nashorn.TestNashorn',
        nonZeroIsFatal=nonZeroIsFatal,
        cwd=_location
    )

def testv8(args, nonZeroIsFatal=True):
    """run the testV8 conformance suite"""
    _location = join(_suite.dir, 'lib', 'testv8')
    _default_vm_args = [
        '-Dtruffle.js.V8LegacyConst=true',
        '-Dtruffle.js.TestV8Mode=true',
    ]
    _stack_size = '2m' if mx.get_arch() is 'sparcv9' else '1m'
    _run_test_suite(
        location=_location,
        library_names=['TESTV8'],
        custom_args=args,
        default_vm_args=_default_vm_args,
        max_heap='4g',
        stack_size=_stack_size,
        main_class='com.oracle.truffle.js.test.external.testv8.TestV8',
        nonZeroIsFatal=nonZeroIsFatal,
        cwd=_suite.dir
    )

def testinstrumentation(args, nonZeroIsFatal=True):
    unittest(['--enable-timing', '--very-verbose', 'com.oracle.truffle.js.test.instrumentation'])

def deploy_binary_if_master(args):
    """If the active branch is 'master', deploy binaries for the primary suite to remote maven repository."""
    primary_branch = 'master'
    _, vc_root = mx.VC.get_vc_root(_suite.dir)
    active_branch = mx.VC.get_vc(vc_root).active_branch(_suite.dir)
    deploy_binary = mx.command_function('deploy-binary')
    if active_branch == primary_branch:
        return deploy_binary(args)
    else:
        mx.warn('The active branch is "%s". Binaries are deployed only if the active branch is "%s".' % (active_branch, primary_branch))
        return 0


mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='Graal.js',
    short_name='js',
    license_files=[],
    third_party_license_files=[],
    truffle_jars=[
        'graal-js:GRAALJS',
        'graal-js:ICU4J',
        'mx:ASM_DEBUG_ALL',
    ],
    support_distributions=[
        'graal-js:GRAALJS_GRAALVM_SUPPORT',
        'graal-js:ICU4J-DIST'
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination='bin/<exe:js>',
            jar_distributions=['graal-js:GRAALJS_LAUNCHER'],
            main_class='com.oracle.truffle.js.shell.JSLauncher',
            build_args=['--language:js']
        )
    ],
    boot_jars=['graal-js:GRAALJS_SCRIPTENGINE']
))

mx.update_commands(_suite, {
    'deploy-binary-if-master' : [deploy_binary_if_master, ''],
    'js' : [js, '[JS args|VM options]'],
    'nashorn' : [nashorn, '[JS args|VM options]'],
    'test262': [test262, ''],
    'testnashorn': [testnashorn, ''],
    'testv8': [testv8, ''],
    'testinstrumentation': [testinstrumentation, ''],
    'unpackIcuData': [unpackIcuData, ''],
})
