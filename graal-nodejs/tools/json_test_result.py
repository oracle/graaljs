from datetime import datetime
import os,platform,subprocess
import random
import re
import sys


class JsonTestResultGenerator():
    def __init__(self):
        print("generate json reports.....")
        self.json_result = "["
        self.output_file = self.generate_test_results_path('unittest')
        self.tags = self.get_tags()
        self.hasContent = False

    def generate_test_results_path(self, key=None):
        pattern = os.getenv('MX_TEST_RESULTS_PATTERN')
        if 'XXX' not in pattern:
            print("MX_TEST_RESULTS_PATTERN doesn't contain 'XXX'. If used multiple times, results will probably be overwritten.")
            return pattern
        identifier = ""
        if key:
            identifier = key + "-"
        identifier += datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        identifier += "-{:08x}".format(random.getrandbits(32))
        return re.compile('XXXX*').sub(identifier, pattern, count=1)

    def get_tags(self):
        self.tags = {get_os(), get_arch(), get_java_version(), 'js'}

        extra_tags = os.getenv("MX_TEST_RESULT_TAGS")
        if extra_tags:
            for tag in extra_tags.split(',') :
                self.tags.add(tag)
        return self.tags

    def passedTest(self, testName, time):
        self.record_test_result("SUCCESS", testName, time)

    def failedTest(self, testName, time):
        self.record_test_result("FAILED", testName, time)

    def ignoredTest(self, testName, time):
        self.record_test_result("IGNORED", testName, time)

    def record_test_result(self, result, testName, time):
        if (self.hasContent):
            self.json_result += ','
        self.json_result += '{{"type": "test-result", "test":"{}", "result":"{}", "time":{}, '.format(testName, result, time)
        self.json_result += '"tags":[' + ','.join(['"' + str(t) + '"' for t in self.tags])
        self.json_result += ']}'
        self.hasContent = True

    def generateReportFile(self):
        self.json_result += ']'
        with open(self.output_file,"w") as JSON:
            JSON.write(self.json_result)


def _decode(x):
        return x.decode()


def _check_output_str(*args, **kwargs):
    try:
        return _decode(subprocess.check_output(*args, **kwargs))
    except subprocess.CalledProcessError as e:
        if e.output:
            e.output = _decode(e.output)
        if hasattr(e, 'stderr') and e.stderr:
            e.stderr = _decode(e.stderr)
        raise e


def get_arch():
    machine = platform.uname()[4]
    if machine in ['aarch64']:
        return 'aarch64'
    if machine in ['amd64', 'AMD64', 'x86_64', 'i86pc']:
        return 'amd64'
    if machine in ['sun4v', 'sun4u', 'sparc64']:
        return 'sparcv9'
    if machine == 'i386' and is_darwin():
        try:
            # Support for Snow Leopard and earlier version of MacOSX
            if _check_output_str(['sysctl', '-n', 'hw.cpu64bit_capable']).strip() == '1':
                return 'amd64'
        except OSError:
            # sysctl is not available
            pass
    print('unknown or unsupported architecture: os=' + get_os() + ', machine=' + machine)


def get_java_version():
    try:
      java_home_bin_java = os.path.join(os.getenv("JAVA_HOME"), "bin", "java")
      proc = subprocess.Popen(java_home_bin_java + " -version", stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
      stdout, java_version = proc.communicate()
      java_version_number = java_version.splitlines()[0].split()[2].decode('utf8').strip('"')
      if java_version_number.startswith("1.") :
          return "java" + java_version_number.split(".")[1]
      return "java" + java_version_number.split(".")[0]
    except:
      return "no-java"


def is_darwin():
    return sys.platform.startswith('darwin')


def is_linux():
    return sys.platform.startswith('linux')


def is_openbsd():
    return sys.platform.startswith('openbsd')


def is_sunos():
    return sys.platform.startswith('sunos')


def is_windows():
    return sys.platform.startswith('win32')


def is_cygwin():
    return sys.platform.startswith('cygwin')


def get_os():
    """
    Get a canonical form of sys.platform.
    """
    if is_darwin():
        return 'darwin'
    elif is_linux():
        return 'linux'
    elif is_openbsd():
        return 'openbsd'
    elif is_sunos():
        return 'solaris'
    elif is_windows():
        return 'windows'
    elif is_cygwin():
        return 'cygwin'
    else:
        print('Unknown operating system ' + sys.platform)
