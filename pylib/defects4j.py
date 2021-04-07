import os
from pylib.countmap import CountMap
import time
from func_timeout import func_set_timeout
import func_timeout
from typing import Any, Dict, List, Set
import json
alld4jprojs = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson",
               "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]
project_bug_nums = {"Lang": 65, "Math": 106,
                    "Time": 27, "Closure": 176, "Chart": 26}


def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')


def getd4jprojinfo():
    for proj in alld4jprojs:
        getinstclassinfo(proj)


def getinstclassinfo(proj: str):
    cmdline = f"defects4j query -p {proj} -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger,tests.relevant\"  -o ./d4j_resources/{proj}.csv"
    cmdline += f' > trace/runtimelog/{proj}query.log'
    os.system(cmdline)


def getmetainfo(proj: str, id: str) -> Dict[str, str]:
    ret = {}
    # caching
    print('Checking for metainfo...', end='')
    cachedir = os.path.abspath(f'./d4j_resources/metadata_cached/{proj}')
    if not os.path.exists(cachedir):
        os.mkdir(cachedir)
    cachepath = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.log')
    if os.path.exists(cachepath):
        print('found')
        lines = utf8open(cachepath).readlines()
        for line in lines:
            line = line.strip()
            splits = line.split('=')
            ret[splits[0]] = splits[1]
        return ret

    print('not found. Generating metadata.')
    # if not cached, generate metadata
    workdir = os.path.abspath(
        f'./tmp_checkout/{proj}/{id}')
    if not os.path.exists(workdir):
        checkout(proj, id)
    fields = ['tests.all', 'classes.relevant',
              'tests.trigger', 'tests.relevant']

    print('Exporting metadata')
    for field in fields:
        tmp_logfieldfile = f'{workdir}/{field}.log'
        cmdline = f'defects4j export -p {field} -w {workdir} -o {tmp_logfieldfile}'
        cmdline += f' > trace/runtimelog/{proj}{id}d4j.export.log'
        os.system(cmdline)
        ret[field] = utf8open(tmp_logfieldfile).read().replace('\n', ';')

    print('Instrumenting all test methods')
    cmdline_getallmethods = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.defects4j.Instrumenter" "-Dexec.args={proj} {id}"'
    cmdline_getallmethods += f' > trace/runtimelog/{proj}{id}.instrumenter.log'
    os.system(cmdline_getallmethods)
    allmethodslog = f'./d4j_resources/metadata_cached/{proj}/{id}.alltests.log'
    ret['methods.test.all'] = utf8open(
        allmethodslog).read().replace('\n', ';')
    # write to cache
    print('Writing to cache')
    cachefile = utf8open_w(cachepath)
    for (k, v) in ret.items():
        cachefile.write(f'{k}={v}\n')
    # cleanup
    print('Removing temporary file')
    # os.system(f'rm -rf {workdir}')
    os.system(f'rm {allmethodslog}')
    return ret


def parseprofile(line: str, trigger_tests: Set[str], testmethods: Set[str]):
    line = line[3:]
    sp = line.split('::')
    class_name = sp[0].strip()
    method_name = sp[1].strip()
    is_trigger = (class_name, method_name) in trigger_tests
    is_test = (class_name, method_name) in testmethods
    return class_name, method_name, is_trigger, is_test


def resolve_profile(profile: List[str], classes_relevant: List[str], trigger_tests: List[str], testmethods: List[str]) -> List[str]:
    print(f'parsing profile, length:{len(profile)}')
    print('trigger tests are: ', trigger_tests)
    relevant = []
    relevant_cnt = CountMap()

    # currelevant = False
    trigger_tests_set, trigger_tests_map = parse_trigger_tests(trigger_tests)
    testmethods_set = parse_test_methods(testmethods)
    fail_coverage = get_fail_coverage(
        profile, trigger_tests_set, testmethods_set)
    curclass = ''
    curmethod = ''
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            curclass, curmethod = class_name, method_name
            # currelevant = False
            continue
        # if currelevant:
        #     continue
        # relevant
        if (class_name, method_name) in fail_coverage:
            # FIXME weird use-before-def bug for curclass,curmethod at Math-2.
            curtest = (curclass, curmethod)
            relevant.append(curtest)
            relevant_cnt.add(curclass, curmethod)
            # currelevant = True
    # TODO use relevant_cnt for filtering
    # relevant = list(set(relevant))
    # return sorted(relevant)
    return relevant_cnt.filter(trigger_tests_map)


def get_fail_coverage(profile, trigger_tests_set, testmethods_set):
    fail_coverage = set()
    curtrigger = False
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            #curclass, curmethod = class_name, method_name
            curtrigger = is_trigger
            continue
        if curtrigger:
            fail_coverage.add((class_name, method_name))
            # print(fail_coverage)
    return fail_coverage


def parse_test_methods(testmethods):
    testmethods_set = set()
    for testmethod in testmethods:
        testmethod = testmethod.strip()
        if testmethod == '':
            continue
        sp = testmethod.split('::')
        methods = sp[1].split(',')
        # print(sp[0], sp[1])
        for method in methods:
            method = method.strip()
            if method != '':
                testmethods_set.add((sp[0], method))
    return testmethods_set


def parse_trigger_tests(trigger_tests):
    trigger_tests_set = set()
    trigger_tests_map = {}
    for trigger_test in trigger_tests:
        trigger_test = trigger_test.strip()
        if trigger_test == '':
            continue
        sp = trigger_test.split('::')
        classname = sp[0].strip()
        methodname = sp[1].strip()
        trigger_tests_set.add((classname, methodname))
        if classname in trigger_tests_map:
            if not methodname in trigger_tests_map[classname]:
                trigger_tests_map[classname].append(methodname)
        else:
            trigger_tests_map[classname] = [methodname]
    return trigger_tests_set, trigger_tests_map


def getd4jtestprofile(metadata: Dict[str, str], proj: str, id: str):
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    classes_relevant = metadata['classes.relevant'].strip()
    testmethods = metadata['methods.test.all'].split(';')
    trigger_tests = metadata['tests.trigger'].strip()
    d4jdatafile = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.log')
    checkoutdir = f'tmp_checkout/{proj}/{id}'

    profile_result = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.profile.log')
    print('checking profiling result...', end='')
    if(os.path.exists(profile_result)):
        print('found')
        tmpstr = utf8open(profile_result).read()
        return json.loads(tmpstr)

    print('not found')
    profile = checkoutdir + '/trace/logs/mytrace/profile.log'
    print('checking profile...', end='')
    if not os.path.exists(profile):
        print('not found. generating...')
        cdcmd = f'cd {checkoutdir} && '
        simplelogcmd = f"defects4j test -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=simplelog=true,d4jdatafile={d4jdatafile}\""
        simplelogcmd += f' > ../../../trace/runtimelog/{proj}{id}.profile.log'
        os.system(cdcmd + simplelogcmd)
    else:
        print('found')
    relevant_testmethods = resolve_profile(
        utf8open(profile).readlines(), classes_relevant.split(';'), trigger_tests.split(';'), testmethods)
    # print(relevant_testmethods)
    print('writing profiling result...')
    json.dump(relevant_testmethods, utf8open_w(profile_result))
    return relevant_testmethods


def getd4jcmdline(proj: str, id: str) -> List[str]:
    metadata = getmetainfo(proj, id)
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    classes_relevant = metadata['classes.relevant'].strip()

    relevant_testmethods = getd4jtestprofile(metadata, proj, id)

    reltest_dict = {}  # {classname : [methodnames]}
    for (cname, mname) in relevant_testmethods:
        if cname in reltest_dict:
            reltest_dict[cname].append(mname)
        else:
            reltest_dict[cname] = [mname]

    relevant_testclass_number = len(reltest_dict)
    relevant_method_number = len(relevant_testmethods)

    instclasses = classes_relevant + ';' + ';'.join(reltest_dict.keys())

    print('writing relevant insts')
    instclasses_cache = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.inst.log')
    utf8open_w(instclasses_cache).write(instclasses)

    instclasses = instclasses.replace(";", ":")

    print(
        f'relevant tests:{relevant_testclass_number} classes, {relevant_method_number} methods')
    # print(reltest_dict)
    # input()

    ret = []
    # for testmethod in testmethods:
    #     if testmethod.strip() == '':
    #         continue
    #     testclassname = testmethod.split('::')[0]
    #     if testclassname in relevant_classes:
    #         app = f"defects4j test -t {testmethod} -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses},d4jdatafile={d4jdatafile}\""
    #         ret.append(app)
    # return ret
    for testclass_rel in reltest_dict:
        app = f"defects4j test -t {testclass_rel}::{','.join(reltest_dict[testclass_rel])} -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses}\""
        app += ' > /dev/null'
        ret.append(app)
    return ret


def checkout(proj: str, id: str):
    checkoutpath = f'./tmp_checkout/{proj}/{id}'
    if not(os.path.exists(checkoutpath)):
        os.makedirs(checkoutpath)
    os.system(
        f'defects4j checkout -p {proj} -v {id}b -w ./tmp_checkout/{proj}/{id}')


def cleanupcheckout(proj: str, id: str):
    checkoutpath = f'./tmp_checkout/{proj}/{id}'
    if (os.path.exists(checkoutpath)):
        os.system(f'rm -rf {checkoutpath}/trace/logs/mytrace/')


def clearcache(proj: str, id: str):
    cachepath = f'./d4j_resources/metadata_cached/{proj}/{id}.*'
    os.system('rm '+cachepath)


def rund4j(proj: str, id: str):
    time_start = time.time()
    workdir = os.path.abspath(f'./tmp_checkout/{proj}/{id}')
    if not os.path.exists(workdir):
        checkout(proj, id)
    cmdlines = getd4jcmdline(proj, id)
    checkoutdir = f'tmp_checkout/{proj}/{id}'
    # cleanup previous log
    previouslog = f'{checkoutdir}/trace/logs/mytrace/all.log'
    if os.path.exists(previouslog):
        print('removing previous trace logs.')
        os.system(f'rm {checkoutdir}/trace/logs/mytrace/all.log')
    cdcmd = f'cd {checkoutdir} && '
    for cmdline in cmdlines:
        testclassname = cmdline.split('::')[0].split(' ')[-1]
        print('testing', testclassname)
        # input()
        os.system(cdcmd + cmdline)
    time_end = time.time()
    print('d4j tracing complete after', time_end-time_start, 'sec')


def rerun(proj: str, id: str):
    clearcache(proj, id)
    cleanupcheckout(proj, id)
    os.system('mvn package -DskipTests')
    rund4j(proj, id)


def parse(proj: str, id: str):
    cmdline = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"'
    os.system(cmdline)


@func_set_timeout(1200)
def fl(proj: str, id: str):
    cleanupcheckout(proj, id)
    clearcache(proj, id)
    rund4j(proj, id)
    parse(proj, id)


def evalproj(proj: str):
    no_oracle = 0
    no_result = 0
    not_listed = 0
    top = []
    for i in range(11):
        top.append(0)
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = eval(proj, str(i))
        if(result > 0 and result <= 10):
            for j in range(result, 11):
                top[j] += 1
        if result == -3:
            no_result += 1
    print(f'top1={top[1]},top3={top[3]},top10={top[10]},failed={no_result}')


def eval(proj: str, id: str):
    try:
        oraclefile = utf8open(f'oracle/ActualFaultStatement/{proj}/{id}')
    except IOError:
        print(f'{proj}{id} has no oracle')
        return -1
    oracle_lines = set()
    for line in oraclefile.readlines():
        sp = line.split('||')
        for oracle in sp:
            oracle_lines.add(oracle.strip())
    try:
        resultfile = utf8open(
            f'trace/logs/mytrace/InfResult-{proj}{id}.log')
    except IOError:
        print(f'{proj}{id} has failed.')
        return -2
    i = 0
    ret = -3
    lines = set()
    for line in resultfile.readlines():
        if(line.strip() == '' or line.startswith('Probabilities:') or line.startswith('Vars:') or line.startswith('Stmts:') or line.startswith('Belief propagation time')):
            continue
        sp = line.split(':')
        if(sp.__len__() < 3):
            print(line)
        classname = sp[0]
        methodname = sp[1]
        sp = sp[2].split('#')
        linenumber = sp[1]
        fullname = f'{classname}.{methodname}:{linenumber}'
        if fullname in lines:
            continue
        lines.add(fullname)
        i += 1
        if fullname in oracle_lines:
            ret = i
            break
    print(f'{proj}{id} result ranking: {ret}')
    return ret
