#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import mx, mx_gate, mx_subst, mx_sdk, mx_graal_js, os, tarfile, tempfile

import mx_graal_nodejs_benchmark

from mx import BinarySuite, TimeStampFile
from mx_gate import Task
from argparse import ArgumentParser
from os.path import exists, join, isdir

_suite = mx.suite('graal-nodejs')
_currentOs = mx.get_os()
_currentArch = mx.get_arch()
_jdkHome = None
_config_files = [join(_suite.dir, f) for f in ('configure', 'configure.py')]
_generated_config_files = [join(_suite.dir, f) for f in ('config.gypi', 'config.status', 'configure.pyc', 'config.mk', 'icu_config.gypi')]

class GraalNodeJsTags:
    allTests = 'all'
    unitTests = 'unit'
    jniProfilerTests = 'jniprofiler'

def _graal_nodejs_post_gate_runner(args, tasks):
    _setEnvVar('NODE_INTERNAL_ERROR_CHECK', 'true')
    with Task('UnitTests', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.unitTests]) as t:
        if t:
            _setEnvVar('NODE_JVM_CLASSPATH', mx.distribution('graal-js:TRUFFLE_JS_TESTS').path)
            commonArgs = ['-ea', '-esa']
            unitTestDir = join('test', 'graal')
            mx.run(['rm', '-rf', 'node_modules', 'build'], cwd=unitTestDir)
            npm(['--scripts-prepend-node-path=auto', 'install', '--nodedir=' + _suite.dir] + commonArgs, cwd=unitTestDir)
            npm(['--scripts-prepend-node-path=auto', 'test'] + commonArgs, cwd=unitTestDir)

    with Task('TestNpm', tasks, tags=[GraalNodeJsTags.allTests]) as t:
        if t:
            tmpdir = tempfile.mkdtemp()
            try:
                npm(['init', '-y'], cwd=tmpdir)
                npm(['--scripts-prepend-node-path=auto', 'install', '--nodedir=' + _suite.dir, '--build-from-source', 'microtime'], cwd=tmpdir)
                node(['-e', 'console.log(require("microtime").now());'], cwd=tmpdir)
            finally:
                mx.rmtree(tmpdir)

    with Task('JniProfilerTests', tasks, tags=[GraalNodeJsTags.allTests, GraalNodeJsTags.jniProfilerTests]) as t:
        if t:
            commonArgs = ['-ea', '-esa']
            unitTestDir = join(mx.project('com.oracle.truffle.trufflenode.jniboundaryprofiler').dir, 'tests')
            mx.run(['rm', '-rf', 'node_modules', 'build'], cwd=unitTestDir)
            npm(['--scripts-prepend-node-path=auto', 'install', '--nodedir=' + _suite.dir] + commonArgs, cwd=unitTestDir)
            node(['-profile-native-boundary', 'test.js'] + commonArgs, cwd=unitTestDir)

mx_gate.add_gate_runner(_suite, _graal_nodejs_post_gate_runner)

def python_cmd():
    if _currentOs == 'windows':
        return 'python.exe'
    else:
        return join(_suite.mxDir, 'python2', 'python')


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

    def build(self):
        pre_ts = GraalNodeJsBuildTask._get_newest_ts(self.subject.getResults(), fatalIfMissing=False)

        pythonPath = join(_suite.mxDir, 'python2')
        prevPATH = os.environ['PATH']
        _setEnvVar('PATH', "%s:%s" % (pythonPath, prevPATH))

        debug_mode = hasattr(self.args, 'debug') and self.args.debug
        debug = ['--debug'] if debug_mode else []
        shared_library = ['--enable-shared-library'] if hasattr(self.args, 'sharedlibrary') and self.args.sharedlibrary else []

        if _currentOs == 'windows':
            _mxrun([join(_suite.dir, 'vcbuild.bat'),
                    'projgen',
                    'no-cctest',
                    'noetw',
                    'nosnapshot',
                    'java-home', _getJdkHome()
                    ] + debug + shared_library,
                   cwd=_suite.dir, verbose=True)
        else:
            newest_config_file_ts = GraalNodeJsBuildTask._get_newest_ts(_config_files, fatalIfMissing=True)
            newest_generated_config_file_ts = GraalNodeJsBuildTask._get_newest_ts(_generated_config_files, fatalIfMissing=False)
            # Lazily generate config files only if `configure` and `configure.py` are older than the files they generate.
            # If we don't do this, the `Makefile` always considers `config.gypi` out of date, triggering a second, unnecessary configure.
            lazy_generator = ['--lazy-generator'] if newest_generated_config_file_ts.isNewerThan(newest_config_file_ts) else []

            _mxrun([join(_suite.dir, 'configure'),
                    '--partly-static',
                    '--without-dtrace',
                    '--without-snapshot',
                    '--without-node-snapshot',
                    '--without-node-code-cache',
                    '--java-home', _getJdkHome()
                    ] + debug + shared_library + lazy_generator,
                   cwd=_suite.dir, verbose=True)

            verbose = 'V={}'.format('1' if mx.get_opts().verbose else '')
            _mxrun([mx.gmake_cmd(), '-j%d' % self.parallelism, verbose], cwd=_suite.dir, verbose=True)

        # put headers for native modules into out/headers
        _setEnvVar('HEADERS_ONLY', '1')
        out = None if mx.get_opts().verbose else open(os.devnull, 'w')
        _mxrun([python_cmd(), join('tools', 'install.py'), 'install', join('out', 'headers'), '/'], out=out)

        post_ts = GraalNodeJsBuildTask._get_newest_ts(self.subject.getResults(), fatalIfMissing=True)
        mx.logv('Newest time-stamp before building: {}\nNewest time-stamp after building: {}\nHas built? {}'.format(pre_ts, post_ts, post_ts.isNewerThan(pre_ts)))
        built = post_ts.isNewerThan(pre_ts)
        if built and _currentOs == 'darwin':
            nodePath = join(_suite.dir, 'out', 'Debug' if debug_mode else 'Release', 'node')
            _mxrun(['install_name_tool', '-add_rpath', join(_getJdkHome(), 'jre', 'lib'), '-add_rpath', join(_getJdkHome(), 'lib'), nodePath], verbose=True)
        return built

    def needsBuild(self, newestInput):
        return (True, None)  # Always try to build

    def clean(self, forBuild=False):
        if not forBuild:
            if _currentOs == 'windows':
                mx.run([join('.', 'vcbuild.bat'),
                        'clean',
                        'java-home', _getJdkHome()
                    ], cwd=_suite.dir)
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
        super(PreparsedCoreModulesProject, self).__init__(suite, name, deps, workingSets, theLicense)
        self.subDir = args.pop('subDir')
        assert 'prefix' in args
        assert 'outputDir' in args

    def getBuildTask(self, args):
        return PreparsedCoreModulesBuildTask(self, args, 1)

    def output_dir(self):
        return self.get_output_root()

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
        if localNewestInput.isNewerThan(newestInput):
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

        brokenModules = [                                        # Uses:
            'assert.js',                                         # await
            join('internal', 'child_process', 'serialization.js'), # yield
            join('internal', 'fs', 'dir.js'),                    # await
            join('internal', 'fs', 'promises.js'),               # await
            join('internal', 'modules', 'esm', 'get_source.js'), # await
            join('internal', 'modules', 'esm', 'loader.js'),     # await
            join('internal', 'modules', 'esm', 'module_job.js'), # await
            join('internal', 'modules', 'esm', 'translators.js'),# await
            join('internal', 'process', 'esm_loader.js'),        # await
            join('internal', 'process', 'execution.js'),         # await
            join('internal', 'readline', 'utils.js'),            # yield
            join('internal', 'streams', 'buffer_list.js'),       # yield
            join('internal', 'streams', 'from.js'),              # await
            join('internal', 'vm', 'module.js'),                 # await
        ]

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
        if _currentOs != 'windows':
            macroFiles.append(join('tools', 'js2c_macros', 'notrace_macros.py'))

        mx.run([python_cmd(), join('tools', 'expand-js-modules.py'), outputDir] + [join('lib', m) for m in moduleSet] + macroFiles,
               cwd=_suite.dir)
        if not (hasattr(self.args, "jdt") and self.args.jdt and not self.args.force_javac):
            mx.run_java(['-cp', mx.classpath([snapshotToolDistribution]), mx.distribution(snapshotToolDistribution).mainClass,
                     '--binary', '--outdir=' + outputDirBin, '--indir=' + outputDirBin] + ['--file=' + m for m in moduleSet],
                    cwd=outputDirBin)
        mx.run([python_cmd(), join(_suite.dir, 'tools', 'snapshot2c.py'), 'node_snapshots.h'] + [join('lib', m + '.bin') for m in moduleSet],
               cwd=outputDir)

    def clean(self, forBuild=False):
        outputDir = self.subject.output_dir()
        if not forBuild and os.path.exists(outputDir):
            mx.rmtree(outputDir)

def node_gyp(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node([join(_suite.dir, 'deps', 'npm', 'node_modules', 'node-gyp', 'bin', 'node-gyp.js')] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def npm(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node([join(_suite.dir, 'deps', 'npm', 'bin', 'npm-cli.js')] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def run_nodejs(vmArgs, runArgs, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return node(vmArgs + runArgs, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def node(args, add_graal_vm_args=True, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    return mx.run(prepareNodeCmdLine(args, add_graal_vm_args), nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=cwd)

def testnode(args, nonZeroIsFatal=True, out=None, err=None, cwd=None):
    mode, vmArgs, progArgs = setupNodeEnvironment(args)
    if mode == 'Debug':
        progArgs += ['-m', 'debug']
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(['-ea', '-esa', '-Xrs', '-Xmx8g'] + vmArgs))
    _setEnvVar('NODE_STACK_SIZE', '4000000')
    _setEnvVar('NODE_INTERNAL_ERROR_CHECK', 'true')
    return mx.run([python_cmd(), join('tools', 'test.py')] + progArgs, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, cwd=(_suite.dir if cwd is None else cwd))

def setLibraryPath(additionalPath=None):
    javaHome = _getJdkHome()

    if _currentOs == 'darwin':
        libraryPath = join(javaHome, 'jre', 'lib')
    elif _currentOs == 'solaris':
        libraryPath = join(javaHome, 'jre', 'lib', 'sparcv9')
    elif _currentOs == 'linux' and _currentArch == 'sparcv9':
        libraryPath = join(javaHome, 'jre', 'lib', 'sparcv9')
    else:
        libraryPath = join(javaHome, 'jre', 'lib', 'amd64')

    libraryPath += ':' + join(javaHome, 'lib')

    if additionalPath:
        libraryPath += ':' + additionalPath

    if 'LD_LIBRARY_PATH' in os.environ:
        libraryPath += ':' + os.environ['LD_LIBRARY_PATH']

    _setEnvVar('LD_LIBRARY_PATH', libraryPath)

def setupNodeEnvironment(args, add_graal_vm_args=True):
    args = args if args else []
    mode, vmArgs, progArgs = _parseArgs(args)
    setLibraryPath()

    if mx.suite('vm', fatalIfMissing=False) is not None and mx.suite('substratevm', fatalIfMissing=False) is not None:
        _prepare_svm_env()
        return mode, vmArgs, progArgs

    if mx.suite('vm', fatalIfMissing=False) is not None or mx.suite('substratevm', fatalIfMissing=False) is not None:
        mx.warn("Running on the JVM.\nIf you want to run on SubstrateVM, you need to dynamically import both '/substratevm' and '/vm'.\nExample: 'mx --env svm node'")

    javaHome = _getJdkHome()
    _setEnvVar('JAVA_HOME', javaHome)
    if mx.suite('compiler', fatalIfMissing=False) is None:
        _setEnvVar('GRAAL_SDK_JAR_PATH', mx.distribution('sdk:GRAAL_SDK').path)
    _setEnvVar('LAUNCHER_COMMON_JAR_PATH', mx.distribution('sdk:LAUNCHER_COMMON').path)
    _setEnvVar('TRUFFLENODE_JAR_PATH', mx.distribution('TRUFFLENODE').path)
    node_jvm_cp = (os.environ['NODE_JVM_CLASSPATH'] + os.pathsep) if 'NODE_JVM_CLASSPATH' in os.environ else ''
    node_cp = node_jvm_cp + mx.classpath(['TRUFFLENODE'] + (['tools:CHROMEINSPECTOR', 'tools:TRUFFLE_PROFILER', 'tools:AGENTSCRIPT'] if mx.suite('tools', fatalIfMissing=False) is not None else []))
    _setEnvVar('NODE_JVM_CLASSPATH', node_cp)

    prevPATH = os.environ['PATH']
    _setEnvVar('PATH', "%s:%s" % (join(_suite.mxDir, 'fake_launchers'), prevPATH))

    if add_graal_vm_args:
        if mx.suite('graal-enterprise', fatalIfMissing=False):
            # explicitly require the enterprise compiler configuration
            vmArgs += ['-Dgraal.CompilerConfiguration=enterprise']
        if mx.suite('compiler', fatalIfMissing=False):
            vmArgs += ['-Djvmci.Compiler=graal', '-XX:+UnlockExperimentalVMOptions', '-XX:+EnableJVMCI']

    if isinstance(_suite, BinarySuite):
        mx.logv('%s is a binary suite' % _suite.name)
        tarfilepath = mx.distribution('TRUFFLENODE_GRAALVM_SUPPORT').path
        with tarfile.open(tarfilepath, 'r:') as tar:
            mx.logv('Extracting {} to {}'.format(tarfilepath, _suite.dir))
            tar.extractall(_suite.dir)

    return mode, vmArgs, progArgs

def makeInNodeEnvironment(args):
    argGroups = setupNodeEnvironment(args)
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(argGroups[1]))
    if _currentOs == 'windows':
        _mxrun([join('.', 'vcbuild.bat'),
                'noprojgen',
                'nobuild',
                'java-home', _getJdkHome()
            ] + argGroups[2], cwd=_suite.dir)
    else:
        makeCmd = mx.gmake_cmd()
        if _currentOs == 'solaris':
            # we have to use GNU make and cp because the Solaris versions
            # do not support options used by Node.js Makefile and gyp files
            _setEnvVar('MAKE', makeCmd)
            _mxrun(['sh', '-c', 'ln -s `which gcp` ' + join(_suite.dir, 'cp')])
            prevPATH = os.environ['PATH']
            _setEnvVar('PATH', "%s:%s" % (_suite.dir, prevPATH))
        _mxrun([makeCmd] + argGroups[2], cwd=_suite.dir)
        if _currentOs == 'solaris':
            _mxrun(['rm', 'cp'])

def prepareNodeCmdLine(args, add_graal_vm_args=True):
    '''run a Node.js program or shell
        --debug to run in debug mode (provided that you build it)
    '''
    mode, vmArgs, progArgs = setupNodeEnvironment(args, add_graal_vm_args)
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(vmArgs))

    if _currentOs == 'windows':
        return [join(_suite.dir, mode, 'node.exe')] + progArgs
    else:
        return [join(_suite.dir, 'out', mode, 'node')] + progArgs

def parse_js_args(args):
    vmArgs, progArgs = mx_graal_js.parse_js_args(args)

    profileJniArg = '-profile-native-boundary'
    if profileJniArg in progArgs:
        mx.log('Running with native profiling agent enabled. The argument is handled by mx and not passed as program argument')
        progArgs.remove(profileJniArg)
        vmArgs += ['-javaagent:{}'.format(mx.distribution('TRUFFLENODE_JNI_BOUNDARY_PROFILER').path)]

    return vmArgs, progArgs

def _mxrun(args, cwd=_suite.dir, verbose=False, out=None):
    if verbose:
        mx.log('Running \'{}\''.format(' '.join(args)))
    status = mx.run(args, nonZeroIsFatal=False, cwd=cwd, out=out)
    if status:
        mx.abort(status)

def _setEnvVar(name, val):
    if val:
        mx.logv('Setting environment variable %s=%s' % (name, val))
        os.environ[name] = val

def _getJdkHome():
    global _jdkHome
    if not _jdkHome:
        _jdkHome = mx.get_env('JAVA_HOME')
    return _jdkHome

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
    libpolyglot_filename = mx.add_lib_suffix(mx.add_lib_prefix('polyglot'))
    libpolyglots = [join(graalvm_home, directory, libpolyglot_filename) for directory in [join('jre', 'lib', 'polyglot'), join('lib', 'polyglot')]]
    libpolyglot = None
    for candidate in libpolyglots:
        if exists(candidate):
            libpolyglot = candidate
    if libpolyglot is None:
        mx.abort("Cannot find polyglot library in '{}'.\nDid you forget to build it (e.g., using 'mx --env svm build')?".format(libpolyglots))
    _setEnvVar('NODE_JVM_LIB', libpolyglot)
    _setEnvVar('ICU4J_DATA_PATH', join(mx.suite('graal-js').dir, 'lib', 'icu4j', 'icudt'))

def mx_post_parse_cmd_line(args):
    mx_graal_nodejs_benchmark.register_nodejs_vms()

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='Graal.nodejs',
    short_name='njs',
    dir_name='js',
    license_files=[],
    third_party_license_files=[],
    truffle_jars=['graal-nodejs:TRUFFLENODE'],
    support_distributions=['graal-nodejs:TRUFFLENODE_GRAALVM_SUPPORT'],
    provided_executables=[
        join('bin', 'node'),
        join('bin', 'npm'),
    ],
    polyglot_lib_build_args=[
        "-H:+ReportExceptionStackTraces",
        "-H:JNIConfigurationResources=svmnodejs.jniconfig,svmnodejs_jdkspecific.jniconfig",
        "-H:ReflectionConfigurationResources=svmnodejs.reflectconfig",
    ],
    polyglot_lib_jar_dependencies=[
        "graal-nodejs:TRUFFLENODE"
    ],
    has_polyglot_lib_entrypoints=True,
    installable=False,
))


mx.update_commands(_suite, {
    'node' : [node, ''],
    'npm' : [npm, ''],
    'node-gyp' : [node_gyp, ''],
    'testnode' : [testnode, ''],
    'makeinnodeenv' : [makeInNodeEnvironment, ''],
})
