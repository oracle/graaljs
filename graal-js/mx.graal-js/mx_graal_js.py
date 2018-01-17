#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
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

import os, zipfile, re, shutil
from os.path import join, exists, isdir, getmtime

import mx
from mx_gate import Task, add_gate_runner

_suite = mx.suite('graal-js')

class GraalJsDefaultTags:
    basic_tests = 'basic_tests'
    all_tests = 'all_tests'

def _graal_js_gate_runner(args, tasks):
    with Task('TestJSCommand', tasks, tags=[GraalJsDefaultTags.basic_tests, GraalJsDefaultTags.all_tests]) as t:
        if t:
            js(['-Dtruffle.js.ProfileTime=true', '-e', '""'])

add_gate_runner(_suite, _graal_js_gate_runner)

class GraalJsProject(mx.ArchivableProject, mx.ClasspathDependency):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        super(GraalJsProject, self).__init__(suite, name, deps, workingSets, theLicense)
        assert 'prefix' in args
        assert 'outputDir' in args

    def classpath_repr(self, resolve=True):
        return self.output_dir()

    def getBuildTask(self, args):
        return GraalJsBuildTask(self, args, 1)

    def get_output_root(self):
        return join(self.dir, self.outputDir)

    def output_dir(self):
        return join(self.get_output_root(), "bin")

    def archive_prefix(self):
        return self.prefix

    def getResults(self):
        return mx.ArchivableProject.walk(self.output_dir())

class GraalJsBuildTask(mx.ArchivableBuildTask):
    def __str__(self):
        return 'Snapshotting {}'.format(self.subject)

    def needsBuild(self, newestInput):
        return (False, 'This project does not contain files')

    def newestOutput(self):
        return mx.TimeStampFile.newest(self.subject.getResults())

    def build(self):
        if hasattr(self.args, "jdt") and self.args.jdt and not self.args.force_javac:
            return
        _output_dir = join(_suite.dir, self.subject.outputDir)
        cp = mx.classpath('com.oracle.truffle.js.snapshot')
        tool_main_class = 'com.oracle.truffle.js.snapshot.SnapshotTool'

        _output_dir_bin = join(_output_dir, "bin")
        mx.ensure_dir_exists(_output_dir_bin)
        mx.run_java(['-cp', cp, tool_main_class, '--binary', '--internal'] + ['--outdir=' + _output_dir_bin], cwd=_output_dir_bin)
        _output_dir_src_gen = join(_output_dir, "src_gen")
        mx.ensure_dir_exists(_output_dir_src_gen)
        mx.run_java(['-cp', cp, tool_main_class, '--java', '--internal'] + ['--outdir=' + _output_dir_src_gen], cwd=_output_dir_src_gen)

        compliance = mx.JavaCompliance("1.8")
        jdk = mx.get_jdk(compliance, tag=mx.DEFAULT_JDK_TAG)

        java_file_list = []
        for root, _, files in os.walk(_output_dir_src_gen, followlinks=True):
            java_file_list += [join(root, name) for name in files if name.endswith('.java')]

        java_file_list = sorted(java_file_list)  # for reproducibility
        mx.run([jdk.javac, '-source', str(compliance), '-target', str(compliance), '-classpath', mx.classpath('com.oracle.truffle.js.parser'), '-d', _output_dir_bin] + java_file_list)

    def clean(self, forBuild=False):
        _output_dir = join(_suite.dir, self.subject.outputDir)
        if exists(_output_dir):
            mx.rmtree(_output_dir)

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

def _append_default_js_vm_args(vm_args, min_heap='2g', max_heap='2g', stacksize=_default_stacksize()):
    if not any(x.startswith('-Xm') for x in vm_args):
        if min_heap:
            vm_args += ['-Xms' + min_heap]
        if max_heap:
            vm_args += ['-Xmx' + max_heap]
    if stacksize and not any(x.startswith('-Xss') for x in vm_args):
        vm_args += ['-Xss' + stacksize]
    return vm_args

def _js_cmd_line(args, main_class, default_cp=None, append_default_args=True):
    _vm_args, _js_args = parse_js_args(args, default_cp=default_cp)
    if append_default_args:
        _vm_args = _append_default_js_vm_args(_vm_args)
    return _vm_args + [main_class] + _js_args

def graaljs_cmd_line(args, append_default_args=True):
    return _js_cmd_line(args + ['-Dtruffle.js.BindProgramResult=false'], main_class=mx.distribution('GRAALJS_LAUNCHER').mainClass, default_cp=[mx.classpath(['GRAALJS_LAUNCHER', 'GRAALJS'])], append_default_args=append_default_args)

def js(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    """Run the REPL or a JavaScript program with Graal.js"""
    return mx.run_java(graaljs_cmd_line(args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def nashorn(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    """Run the REPL or a JavaScript program with Nashorn"""
    return mx.run_java(_js_cmd_line(args, main_class='jdk.nashorn.tools.Shell'), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

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

mx.update_commands(_suite, {
    'js' : [js, '[JS args|VM options]'],
    'nashorn' : [nashorn, '[JS args|VM options]'],
    'unpackIcuData': [unpackIcuData, ''],
})
