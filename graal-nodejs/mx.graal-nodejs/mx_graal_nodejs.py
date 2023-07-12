#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

import mx, mx_gate, mx_subst, mx_sdk, mx_sdk_vm, mx_graal_js, os, tempfile

import mx_graal_nodejs_benchmark

from mx import TimeStampFile
from mx_gate import Task
from argparse import ArgumentParser
from os.path import exists, join, isdir, pathsep, sep
from mx_graal_js import get_jdk

_suite = mx.suite('graal-nodejs')
_current_os = mx.get_os()
_is_windows = _current_os == 'windows'
_current_arch = mx.get_arch()
_config_files = [join(_suite.dir, f) for f in ('configure', 'configure.py')]
_generated_config_files = [join(_suite.dir, f) for f in ('config.gypi', 'config.status', 'configure.pyc', 'config.mk', 'icu_config.gypi')]

class GraalNodeJsTags:
    allTests = 'all'
    unitTests = 'unit'
    windows = 'windows'  # we cannot run `node-gyp` in our CI unless we install the "Visual Studio Build Tools" (using the "Visual C++ build tools" workload)
    coverage = 'coverage'
    testnode = 'testnode'

def _graal_nodejs_post_gate_runner(args, tasks):
    _setEnvVar('NODE_INTERNAL_ERROR_CHECK', 'true')
    with Task('UnitTests', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.unitTests, GraalNodeJsTags.coverage], report=True) as t:
        if t:
            _setEnvVar('NODE_JVM_CLASSPATH', mx.distribution('graal-js:TRUFFLE_JS_TESTS').path)
            commonArgs = ['-ea', '-esa']
            unitTestDir = join(_suite.dir, 'test', 'graal')
            for dir_name in 'node_modules', 'build':
                p = join(unitTestDir, dir_name)
                if exists(p):
                    mx.rmtree(p)

            jsonResultsFile = tempfile.NamedTemporaryFile(delete=False, suffix='.json.gz').name
            testArgs = ['--', '--json-results=' + jsonResultsFile]
            try:
                npm(['--scripts-prepend-node-path=auto', 'install', '--nodedir=' + _suite.dir] + commonArgs, cwd=unitTestDir)
                npm(['--scripts-prepend-node-path=auto', 'test'] + commonArgs + testArgs, cwd=unitTestDir)
                if mx.suite('wasm', fatalIfMissing=False):
                    npm(['--scripts-prepend-node-path=auto', 'run', 'testwasm'] + commonArgs + testArgs, cwd=unitTestDir)
                    # test that WebAssembly can be enabled using env. variables
                    _setEnvVar('NODE_OPTIONS', '--polyglot')
                    _setEnvVar('NODE_POLYGLOT_OPTIONS', '--js.webassembly --experimental-options')
                    node(commonArgs + ['-e', 'console.log(WebAssembly)'])
                    # check that fetch API is available when WebAssembly is available
                    node(commonArgs + ['-e', 'FormData'])
                    # run selected Node.js tests related to WebAssembly
                    wasm_tests = [
                        'test-fetch.mjs',
                        'test-fetch-disabled.mjs',
                        'test-wasm-simple.js',
                        'test-wasm-web-api.js',
                        'test-whatwg-webstreams-transfer.js',
                        'test-worker-message-port-wasm-module.js'
                    ]
                    for test in wasm_tests:
                        node(commonArgs + [join(_suite.dir, 'test', 'parallel', test)])
                    _setEnvVar('NODE_OPTIONS', '')
                    _setEnvVar('NODE_POLYGLOT_OPTIONS', '')

                mx_gate.make_test_report(jsonResultsFile, task=t.title)
            finally:
                os.unlink(jsonResultsFile)

    with Task('TestNpm', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.windows]) as t:
        if t:
            tmpdir = tempfile.mkdtemp()
            try:
                npm(['init', '-y'], cwd=tmpdir)
                npm(['install', '--scripts-prepend-node-path=true', 'microtime'], cwd=tmpdir)
                node(['-e', 'console.log(require("microtime").now());'], cwd=tmpdir)
            finally:
                mx.rmtree(tmpdir, ignore_errors=True)

    with Task('TestNpx', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.windows]) as t:
        if t:
            npx(['--package=cowsay', '--yes', 'cowsay', 'GraalVM rules!'])

    with Task('TestNodeInstrument', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.windows, GraalNodeJsTags.coverage]) as t:
        if t:
            testnodeInstrument([])

    suite = os.getenv('NODE_SUITE')
    if suite is not None:
        with Task('TestNode:' + suite, tasks, tags=[GraalNodeJsTags.testnode], report=True) as t:
            if t:
                max_heap = os.getenv('NODE_MAX_HEAP')
                part = os.getenv('NODE_PART')
                testnode((['-Xmx' + max_heap] if max_heap else []) +
                        [suite] +
                        ([part] if part else []))

mx_gate.add_gate_runner(_suite, _graal_nodejs_post_gate_runner)

class GraalNodeJsProject(mx.NativeProject):  # pylint: disable=too-many-ancestors
    def __init__(self, suite, name, deps, workingSets, results, output, **args):
        self.suite = suite
        self.name = name
        mx.NativeProject.__init__(self, suite, name, '', [], deps, workingSets, results, output, suite.dir, **args)

    def getBuildTask(self, args):
        return GraalNodeJsBuildTask(self, args)

    def getResults(self, replaceVar=mx_subst.results_substitutions):
        res = super(GraalNodeJsProject, self).getResults(replaceVar)
        for result in res:
            if not exists(result):
                mx.warn('GraalNodeJsProject %s in %s did not find %s' % (self.name, self.suite.name, result))
        return res


class GraalNodeJsBuildTask(mx.NativeBuildTask):
    def __init__(self, project, args):
        mx.NativeBuildTask.__init__(self, args, project)
        self._debug_mode = hasattr(self.args, 'debug') and self.args.debug
        self._out_dir = join(_suite.dir, 'out')
        self._build_dir = join(_suite.dir, 'out', 'Debug' if self._debug_mode else 'Release')

    def build(self):
        pre_ts = GraalNodeJsBuildTask._get_newest_ts(self.subject.getResults(), fatalIfMissing=False)

        build_env = os.environ.copy()

        debug = ['--debug'] if self._debug_mode else []
        shared_library = ['--enable-shared-library'] if hasattr(self.args, 'sharedlibrary') and self.args.sharedlibrary else []

        newest_config_file_ts = GraalNodeJsBuildTask._get_newest_ts(_config_files, fatalIfMissing=True)
        newest_generated_config_file_ts = GraalNodeJsBuildTask._get_newest_ts(_generated_config_files, fatalIfMissing=False)
        # Lazily generate config files only if `configure` and `configure.py` are older than the files they generate.
        # If we don't do this, the `Makefile` always considers `config.gypi` out of date, triggering a second, unnecessary configure.
        lazy_generator = ['--lazy-generator'] if newest_generated_config_file_ts.isNewerThan(newest_config_file_ts) else []

        if _is_windows:
            processDevkitRoot(env=build_env)
            _setEnvVar('PATH', pathsep.join([build_env['PATH']] + [mx.library(lib_name).get_path(True) for lib_name in ('NASM', 'NINJA')]), build_env)
            extra_flags = ['--ninja', '--dest-cpu=x64', '--without-etw']
        elif _current_arch == 'aarch64':
            # we do not use compiler recent enough to support neon
            extra_flags = ['--with-arm-fpu=vfp']
        else:
            extra_flags = []

        _mxrun(['python3', join(_suite.dir, 'configure'),
                '--partly-static',
                '--without-dtrace',
                '--without-inspector',
                '--without-node-snapshot',
                '--without-node-code-cache',
                '--java-home', _java_home(forBuild=True)
                ] + debug + shared_library + lazy_generator + extra_flags,
                cwd=_suite.dir, print_cmd=True, env=build_env)

        quiet_build = mx.is_continuous_integration() and not mx.get_opts().verbose
        if _is_windows:
            verbose = ['-v'] if mx.get_opts().verbose else []
            # The custom env is not used to resolve the location of the executable
            _mxrun([join(mx.library('NINJA').get_path(True), 'ninja.exe')] + verbose + ['-j%d' % self.parallelism, '-C', self._build_dir], print_cmd=True, quiet_if_successful=quiet_build, env=build_env)
        else:
            verbose = 'V={}'.format('1' if mx.get_opts().verbose else '')
            _mxrun([mx.gmake_cmd(), '-j%d' % self.parallelism, verbose], cwd=_suite.dir, print_cmd=True, quiet_if_successful=quiet_build, env=build_env)

        # put headers for native modules into out/headers
        _setEnvVar('HEADERS_ONLY', '1', build_env)
        _mxrun(['python3', join('tools', 'install.py'), 'install', join('out', 'headers'), sep], quiet_if_successful=not mx.get_opts().verbose, env=build_env)

        if not _is_windows:
            # copy libjsig.so from the jdk for inclusion in the standalone and `mx node`
            libjsig_name = mx.add_lib_suffix(mx.add_lib_prefix('jsig'))
            mx.ensure_dir_exists(join(self._out_dir, 'lib'))
            mx.copyfile(join(_java_home(forBuild=True), 'lib', libjsig_name), join(self._out_dir, 'lib', libjsig_name))

        post_ts = GraalNodeJsBuildTask._get_newest_ts(self.subject.getResults(), fatalIfMissing=True)
        mx.logv('Newest time-stamp before building: {}\nNewest time-stamp after building: {}\nHas built? {}'.format(pre_ts, post_ts, post_ts.isNewerThan(pre_ts)))
        built = post_ts.isNewerThan(pre_ts)
        return built

    def needsBuild(self, newestInput):
        return (True, None)  # Always try to build

    def clean(self, forBuild=False):
        if not forBuild:
            if _is_windows:
                if exists(self._build_dir):
                    mx.run([join(mx.library('NINJA').extract_path, 'ninja.exe'), '-C', self._build_dir, '-t', 'clean'])
            else:
                mx.run([mx.gmake_cmd(), 'clean'], nonZeroIsFatal=False, cwd=_suite.dir)
            for f in _generated_config_files:
                if exists(f):
                    mx.rmtree(f)

    @staticmethod
    def _get_newest_ts(files, fatalIfMissing=False):
        paths = []
        for f in files:
            if not exists(f):
                mx.abort_or_warn("Result file '{}' does not exist".format(f), fatalIfMissing)
                return TimeStampFile(f)
            if isdir(f):
                for _root, _, _files in os.walk(f):
                    paths += [join(_root, _f) for _f in _files]
            else:
                paths.append(f)
        return TimeStampFile.newest(paths)


class GraalNodeJsArchiveProject(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        for attr in ['outputDir', 'prefix', 'results']:
            setattr(self, attr, args.pop(attr))
            if getattr(self, attr, None) is None:
                mx.abort("Missing '{}' attribute".format(attr), context="GraalNodeJsArchiveProject {}".format(name))
        mx.ArchivableProject.__init__(self, suite, name, deps, workingSets, theLicense)

    def output_dir(self):
        return join(self.dir, self.outputDir)

    def archive_prefix(self):
        return self.prefix

    def getResults(self, replaceVar=mx._replaceResultsVar):
        return [join(self.output_dir(), res) for res in self.results]

class PreparsedCoreModulesProject(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        self.outputDir = join(suite.dir, args['outputDir'])
        self.prefix = args['prefix']
        super(PreparsedCoreModulesProject, self).__init__(suite, name, deps, workingSets, theLicense)

    def getBuildTask(self, args):
        return PreparsedCoreModulesBuildTask(self, args, 1)

    def output_dir(self):
        return self.outputDir

    def archive_prefix(self):
        return self.prefix

    def getResults(self):
        return [join(self.output_dir(), 'node_snapshots.h')]

class PreparsedCoreModulesBuildTask(mx.ArchivableBuildTask):
    def __str__(self):
        return 'Snapshotting {}'.format(self.subject)

    def newestInput(self):
        relInputPaths = [join('lib', m) for m in self.modulesToSnapshot()] + \
                        [join('mx.graal-nodejs', 'mx_graal_nodejs.py'),
                         join('tools', 'js2c.py'),
                         join('tools', 'expand-js-modules.py'),
                         join('tools', 'snapshot2c.py'),
                         join('tools', 'js2c_macros', 'check_macros.py'),
                         join('tools', 'js2c_macros', 'notrace_macros.py')]
        absInputPaths = [join(_suite.dir, p) for p in relInputPaths]
        return mx.TimeStampFile.newest(absInputPaths)

    def needsBuild(self, newestInput):
        localNewestInput = self.newestInput()
        if newestInput is None or localNewestInput.isNewerThan(newestInput):
            newestInput = localNewestInput

        sup = mx.BuildTask.needsBuild(self, newestInput)
        if sup[0]:
            return sup
        reason = mx._needsUpdate(newestInput, self.subject.getResults()[0])
        if reason:
            return (True, reason)
        return (False, None)

    def modulesToSnapshot(self):
        if hasattr(self.args, "jdt") and self.args.jdt and not self.args.force_javac:
            return []

        brokenModules = []

        allModules = []
        modulePath = join(_suite.dir, 'lib')
        for root, _, files in os.walk(modulePath, followlinks=False):
            for name in (f for f in files if f.endswith('.js')):
                relname = os.path.relpath(join(root, name), modulePath)
                allModules.append(relname)

        return set(allModules).difference(set(brokenModules))

    def build(self):
        outputDir = self.subject.output_dir()
        snapshotToolDistribution = 'graal-js:TRUFFLE_JS_SNAPSHOT_TOOL'

        moduleSet = self.modulesToSnapshot()

        outputDirBin = join(outputDir, 'lib')
        mx.ensure_dir_exists(outputDirBin)

        macroFiles = [join('tools', 'js2c_macros', 'check_macros.py')]
        # DTrace is disabled explicitly by the --without-dtrace option
        # ETW is enabled by default only on Windows
        if not _is_windows:
            macroFiles.append(join('tools', 'js2c_macros', 'notrace_macros.py'))

        mx.run(['python3', join('tools', 'expand-js-modules.py'), outputDir] + [join('lib', m) for m in moduleSet] + macroFiles,
               cwd=_suite.dir)
        if not (hasattr(self.args, "jdt") and self.args.jdt and not self.args.force_javac):
            mx.run_java(['-cp', mx.classpath([snapshotToolDistribution]), '-Dpolyglot.engine.WarnInterpreterOnly=false',
                    mx.distribution(snapshotToolDistribution).mainClass,
                    '--binary', '--wrapped', '--outdir=' + outputDirBin, '--indir=' + outputDirBin] + ['--file=' + m for m in moduleSet],
                    cwd=outputDirBin)
        mx.run(['python3', join(_suite.dir, 'tools', 'snapshot2c.py'), 'node_snapshots.h'] + [join('lib', m + '.bin') for m in moduleSet],
               cwd=outputDir)

    def clean(self, forBuild=False):
        outputDir = self.subject.output_dir()
        if not forBuild and os.path.exists(outputDir):
            mx.rmtree(outputDir)

def node_gyp(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node([join(_suite.dir, 'deps', 'npm', 'node_modules', 'node-gyp', 'bin', 'node-gyp.js')] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def npm(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node([join(_suite.dir, 'deps', 'npm', 'bin', 'npm-cli.js')] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def npx(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node([join(_suite.dir, 'deps', 'npm', 'bin', 'npx-cli.js')] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def run_nodejs(vmArgs, runArgs, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node(vmArgs + runArgs, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def node(args, add_graal_vm_args=True, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return mx.run(prepareNodeCmdLine(args, add_graal_vm_args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def testnodeInstrument(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    instrument_cp = mx.classpath(['TRUFFLENODE_TEST'])
    _setEnvVar('NODE_JVM_CLASSPATH', instrument_cp)
    test = join(_suite.dir, 'test', 'graal', 'instrument', 'async-test.js')
    node(['--experimental-options', '--testing-agent', '-ea', test], nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)
    node(['--experimental-options', '--broken-instrument', '-e', '6*7'], nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)
    node(['--experimental-options', '--coverage-like-instrument', '-e', '6*7'], nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def testnode(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    mode, vmArgs, progArgs = setupNodeEnvironment(args)
    if mode == 'Debug':
        progArgs += ['-m', 'debug']
    extraArgs = ['-Xmx8g'] if not any(vmArg.startswith('-Xmx') for vmArg in vmArgs) else []
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(['-ea', '-Xrs'] + extraArgs + vmArgs))
    _setEnvVar('NODE_STACK_SIZE', '4000000')
    _setEnvVar('NODE_INTERNAL_ERROR_CHECK', 'true')
    return mx.run(['python3', join('tools', 'test.py')] + progArgs, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=(_suite.dir if cwd is None else cwd))

def setLibraryPath():
    if _current_os == 'windows':
        library_path = join(_java_home(), 'bin')
    else:
        library_path = join(_java_home(), 'lib')

    if 'LD_LIBRARY_PATH' in os.environ:
        library_path += pathsep + os.environ['LD_LIBRARY_PATH']

    _setEnvVar('LD_LIBRARY_PATH', library_path)

def processDevkitRoot(env=None):
    assert _is_windows
    _env = env or os.environ
    devkit_root = _env.get('DEVKIT_ROOT')
    if devkit_root is not None:
        _setEnvVar('GYP_MSVS_OVERRIDE_PATH', devkit_root, _env)
        _setEnvVar('PATH', '%s%s%s' % (join(devkit_root, 'VC', 'bin', 'x64'), pathsep, _env['PATH']), _env)
        _setEnvVar('WINDOWSSDKDIR', join(devkit_root, '10'), _env)
        _setEnvVar('VCINSTALLDIR', r'{devkit}\VC'.format(devkit=devkit_root))
        _setEnvVar('INCLUDE', r'{devkit}\VC\include;{devkit}\VC\atlmfc\include;{devkit}\10\include\shared;{devkit}\10\include\ucrt;{devkit}\10\include\um;{devkit}\10\include\winrt;{prev}'.format(devkit=devkit_root, prev=_env['INCLUDE']), _env)
        _setEnvVar('LIB', r'{devkit}\VC\lib\x64;{devkit}\VC\atlmfc\lib\x64;{devkit}\10\lib\x64;{prev}'.format(devkit=devkit_root, prev=_env['LIB']), _env)
        devkit_version = _env.get('DEVKIT_VERSION')
        if devkit_version is not None:
            _setEnvVar('GYP_MSVS_VERSION', devkit_version, _env)

def setupNodeEnvironment(args, add_graal_vm_args=True):
    args = args if args else []
    mode, vmArgs, progArgs = _parseArgs(args)
    setLibraryPath()

    if _is_windows:
        processDevkitRoot()

    if mx.suite('vm', fatalIfMissing=False) is not None and mx.suite('substratevm', fatalIfMissing=False) is not None:
        _prepare_svm_env()
        return mode, vmArgs, progArgs

    if mx.suite('vm', fatalIfMissing=False) is not None or mx.suite('substratevm', fatalIfMissing=False) is not None:
        mx.warn("Running on the JVM.\nIf you want to run on SubstrateVM, you need to dynamically import both '/substratevm' and '/vm'.\nExample: 'mx --env svm node'")

    if mx.suite('compiler', fatalIfMissing=False) is None and not any(x.startswith('-Dpolyglot.engine.WarnInterpreterOnly') for x in vmArgs + get_jdk().java_args):
        vmArgs += ['-Dpolyglot.engine.WarnInterpreterOnly=false']

    # if mx.suite('compiler', fatalIfMissing=False) is None:
    #     _setEnvVar('GRAAL_SDK_JAR_PATH', mx.distribution('sdk:GRAAL_SDK').path)
    _setEnvVar('LAUNCHER_COMMON_JAR_PATH', mx.distribution('sdk:LAUNCHER_COMMON').path)
    _setEnvVar('TRUFFLENODE_JAR_PATH', mx.distribution('TRUFFLENODE').path)
    node_jvm_cp = (os.environ['NODE_JVM_CLASSPATH'] + pathsep) if 'NODE_JVM_CLASSPATH' in os.environ else ''
    node_cp = node_jvm_cp + mx.classpath(['TRUFFLENODE']
        + (['tools:CHROMEINSPECTOR', 'tools:TRUFFLE_PROFILER', 'tools:INSIGHT'] if mx.suite('tools', fatalIfMissing=False) is not None else [])
        + (['wasm:WASM'] if mx.suite('wasm', fatalIfMissing=False) is not None else []))
    _setEnvVar('NODE_JVM_CLASSPATH', node_cp)
    _setEnvVar('JAVA_HOME', _java_home())  # when running with the Graal compiler, setting `$JAVA_HOME` to the GraalJDK should be done after calling `mx.classpath()`, which resets `$JAVA_HOME` to the value of the `--java-home` mx cmd line argument

    prevPATH = os.environ['PATH']
    _setEnvVar('PATH', "%s%s%s" % (join(_suite.mxDir, 'fake_launchers'), pathsep, prevPATH))

    return mode, vmArgs, progArgs

def makeInNodeEnvironment(args):
    argGroups = setupNodeEnvironment(args)
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(argGroups[1]))
    if _is_windows:
        _mxrun([join('.', 'vcbuild.bat'),
                'noprojgen',
                'nobuild',
                'java-home', _java_home()
            ] + argGroups[2], cwd=_suite.dir)
    else:
        makeCmd = mx.gmake_cmd()
        if _current_os == 'solaris':
            # we have to use GNU make and cp because the Solaris versions
            # do not support options used by Node.js Makefile and gyp files
            _setEnvVar('MAKE', makeCmd)
            _mxrun(['sh', '-c', 'ln -s `which gcp` ' + join(_suite.dir, 'cp')])
            prevPATH = os.environ['PATH']
            _setEnvVar('PATH', "%s:%s" % (_suite.dir, prevPATH))
        _mxrun([makeCmd] + argGroups[2], cwd=_suite.dir)
        if _current_os == 'solaris':
            _mxrun(['rm', 'cp'])

def prepareNodeCmdLine(args, add_graal_vm_args=True):
    '''run a Node.js program or shell
        --debug to run in debug mode (provided that you build it)
    '''
    mode, vmArgs, progArgs = setupNodeEnvironment(args, add_graal_vm_args)

    if mx_gate.get_jacoco_agent_args():
        # The node launcher (i.e., JNI) does not support @argfile vm args, so we have to expand them to ordinary args
        def expandArgFile(arg):
            if arg.startswith('@'):
                with open(arg[1:], 'r') as f:
                    return f.readlines()
            assert arg[0] == '-', arg
            return [arg]
        def expandArgs(args):
            return [arg for expandedArg in (expandArgFile(arg) for arg in args) for arg in expandedArg]

        # use absolute path for jacoco destfile to allow changing the working directory
        mx_gate.JACOCO_EXEC = join(_suite.dir, 'jacoco.exec')

        vmArgs += expandArgs(mx_gate.get_jacoco_agent_args())

    # inherit any NODE_JVM_OPTIONS from the parent process
    vmArgs += [parentVmArg for parentVmArg in os.environ.get('NODE_JVM_OPTIONS', '').split() if not parentVmArg in vmArgs]

    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(vmArgs))
    return [join(_suite.dir, 'out', mode, 'node')] + progArgs

def parse_js_args(args):
    vmArgs, progArgs = mx_graal_js.parse_js_args(args)
    return vmArgs, progArgs

def _mxrun(args, cwd=_suite.dir, print_cmd=False, quiet_if_successful=False, env=None):
    out = mx.OutputCapture() if quiet_if_successful else None
    if print_cmd:
        mx.log('Running \'{}\''.format(' '.join(args)))
    status = mx.run(args, nonZeroIsFatal=False, cwd=cwd, out=out, err=out, env=env)
    if status:
        if quiet_if_successful:
            mx.log(out.data)
        mx.abort(status)

def _setEnvVar(name, val, env=None):
    _env = env or os.environ
    if val:
        mx.logv('Setting environment variable %s=%s' % (name, val))
        _env[name] = val

def _java_home(forBuild=False):
    return get_jdk(forBuild=forBuild).home

def _has_jvmci(forBuild=False):
    return get_jdk(forBuild=forBuild).tag == 'jvmci'

def _parseArgs(args):
    arguments = list(args)
    debugArg = '--debug'
    if debugArg in arguments:
        mx.log('Running in debug mode. The --debug argument is handled by mx and not passed as program argument')
        arguments.remove(debugArg)
        mode = 'Debug'
    else:
        mode = 'Release'

    vmArgs, progArgs = parse_js_args(arguments)
    if mx.suite('compiler', fatalIfMissing=False):
        import mx_compiler
        vmArgs = mx_compiler._parseVmArgs(vmArgs)
    else:
        vmArgs += mx.java_debug_args()

    for arg in ['-d64', '-server']:
        if arg in vmArgs:
            mx.logv('[_parseArgs] removing {} from vmArgs'.format(arg))
            vmArgs.remove(arg)

    mx.logv('[_parseArgs] mode: %s' % mode)
    mx.logv('[_parseArgs] vmArgs: %s' % vmArgs)
    mx.logv('[_parseArgs] progArgs: %s' % progArgs)

    return mode, vmArgs, progArgs

def overrideBuild():
    def build(args):
        # add custom build arguments
        parser = ArgumentParser(prog='mx build')
        parser.add_argument('--debug', action='store_true', dest='debug', help='build in debug mode')
        mx.build(args, parser)
        return 0

    mx.update_commands(_suite, {
        'build' : [build, ''],
    })

if _suite.primary:
    overrideBuild()

def _prepare_svm_env():
    import mx_vm
    if hasattr(mx_vm, 'graalvm_home'):
        graalvm_home = mx_vm.graalvm_home()
    else:
        import mx_sdk_vm_impl
        graalvm_home = mx_sdk_vm_impl.graalvm_home()
    libgraal_nodejs_filename = mx.add_lib_suffix(mx.add_lib_prefix('graal-nodejs'))
    candidates = [join(graalvm_home, directory, libgraal_nodejs_filename) for directory in [join('jre', 'languages', 'nodejs', 'lib'), join('languages', 'nodejs', 'lib')]]
    libgraal_nodejs = None
    for candidate in candidates:
        if exists(candidate):
            libgraal_nodejs = candidate
    if libgraal_nodejs is None:
        mx.abort("Cannot find graal-nodejs library in '{}'.\nDid you forget to build it (e.g., using 'mx --env svm build')?".format(candidates))
    _setEnvVar('NODE_JVM_LIB', libgraal_nodejs)

def mx_post_parse_cmd_line(args):
    mx_graal_nodejs_benchmark.register_nodejs_vms()

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='Graal.nodejs',
    short_name='njs',
    dir_name='nodejs',
    license_files=[],
    third_party_license_files=[],
    dependencies=[
        'Graal.js',
        'Graal.nodejs license files',
    ],
    truffle_jars=['graal-nodejs:TRUFFLENODE'],
    support_distributions=[
        'graal-nodejs:TRUFFLENODE_GRAALVM_SUPPORT',
    ],
    provided_executables=[
        'bin/<exe:node>',
        'bin/<cmd:npm>',
        'bin/<cmd:npx>',
    ],
    polyglot_lib_build_args=['--language:nodejs'],
    polyglot_lib_jar_dependencies=['graal-nodejs:TRUFFLENODE'],
    library_configs=[
        mx_sdk_vm.LibraryConfig(
            destination='lib/<lib:graal-nodejs>',
            jar_distributions=['graal-nodejs:TRUFFLENODE'],
            build_args=[
                '--tool:all',
                '--language:nodejs',
                '-Dgraalvm.libpolyglot=true',  # `lib:graal-nodejs` should be initialized like `lib:polyglot` (GR-10038)
            ],
            build_args_enterprise=[
                '-H:+AuxiliaryEngineCache',
                '-H:ReservedAuxiliaryImageBytes=2145482548',
            ] if not mx.is_windows() else [],
            home_finder=True,
        ),
    ],
    has_polyglot_lib_entrypoints=True,
    standalone_dir_name='graalnodejs-community-<version>-<graalvm_os>-<arch>',
    standalone_dir_name_enterprise='graalnodejs-<version>-<graalvm_os>-<arch>',
    standalone_dependencies={
        'GraalVM license files': ('', ['GRAALVM-README.md']),
        'Graal.nodejs license files': ('', []),
    },
    standalone_dependencies_enterprise={
        'GraalVM enterprise license files': ('', ['GRAALVM-README.md']),
    },
    installable=True,
    stability="supported",
))

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='Graal.nodejs license files',
    short_name='njsl',
    dir_name='nodejs',
    license_files=['LICENSE_GRAALNODEJS.txt'],
    third_party_license_files=['THIRD_PARTY_LICENSE_GRAALNODEJS.txt'],
    dependencies=[],
    truffle_jars=[],
    support_distributions=[
        'graal-nodejs:TRUFFLENODE_GRAALVM_LICENSES',
    ],
    priority=5,
    installable=True,
    stability="supported",
))


mx.update_commands(_suite, {
    'node' : [node, ''],
    'npm' : [npm, ''],
    'npx' : [npx, ''],
    'node-gyp' : [node_gyp, ''],
    'testnode' : [testnode, ''],
    'testnodeinstrument' : [testnodeInstrument, ''],
    'makeinnodeenv' : [makeInNodeEnvironment, ''],
})
