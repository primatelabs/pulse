#!/usr/bin/env python3
"""
Set up a local Ivy repository for Pulse 2.7.x build.
Populates from the local pulse-2.6.24 installation JARs.
"""

import os
import shutil
import zipfile
import urllib.request
from pathlib import Path

LIB_DIR = Path.home() / "Scratch/pulse-server/pulse-2.6.24/versions/0206024000/lib"
BOOT_JAR = Path.home() / "Scratch/pulse-server/pulse-2.6.24/lib/boot.jar"
PLUGINS_DIR = Path.home() / "Scratch/pulse-server/pulse-2.6.24/versions/0206024000/system/plugins/internal"
REPO_DIR = Path(__file__).parent / "repository"

MAVEN_CENTRAL = "https://repo1.maven.org/maven2"


def download(url, dest):
    dest = Path(dest)
    if dest.exists():
        print(f"  Already exists: {dest.name}")
        return True
    print(f"  Downloading {url} ...")
    try:
        urllib.request.urlretrieve(url, dest)
        print(f"  Downloaded: {dest.name}")
        return True
    except Exception as e:
        print(f"  FAILED to download {url}: {e}")
        return False


def empty_jar(path):
    path = Path(path)
    with zipfile.ZipFile(path, 'w') as zf:
        pass


def make_ivy_xml(org, module, revision, confs, publications):
    """
    confs: list of conf names
    publications: list of dicts with keys:
        name, type (default 'jar'), ext (default 'jar'), conf (list of conf names)
    """
    all_confs = set(confs)
    all_confs.add('default')
    for p in publications:
        for c in p.get('conf', ['default']):
            all_confs.add(c)

    conf_xml = '\n'.join(
        f'        <conf name="{c}" visibility="public"/>'
        for c in sorted(all_confs)
    )

    pub_parts = []
    for p in publications:
        art_confs = ','.join(p.get('conf', ['default']))
        pub_parts.append(
            f'        <artifact name="{p["name"]}" type="{p.get("type","jar")}" '
            f'ext="{p.get("ext","jar")}" conf="{art_confs}"/>'
        )
    pub_xml = '\n'.join(pub_parts)

    return f'''<?xml version="1.0" encoding="UTF-8"?>
<ivy-module version="2.0">
    <info organisation="{org}" module="{module}" revision="{revision}" status="release"/>
    <configurations>
{conf_xml}
    </configurations>
    <publications>
{pub_xml}
    </publications>
</ivy-module>
'''


def setup_module(org, module, revision, publications, extra_confs=None):
    """
    publications: list of dicts:
        name - artifact name
        src  - source JAR path (Path or None for empty stub)
        conf - list of conf names this artifact appears in (default: ['default'])
        type/ext - artifact type/extension
    extra_confs: additional conf names to declare (without artifacts)
    """
    module_dir = REPO_DIR / org / module
    jars_dir = module_dir / "jars"
    jars_dir.mkdir(parents=True, exist_ok=True)

    all_confs = list(extra_confs or [])
    ivy_pubs = []

    for pub in publications:
        src = pub.get('src')
        art_name = pub['name']
        art_ext = pub.get('ext', 'jar')
        art_confs = pub.get('conf', ['default'])
        dest = jars_dir / f"{art_name}-{revision}.{art_ext}"

        ivy_pubs.append({'name': art_name, 'ext': art_ext, 'conf': art_confs})

        if dest.exists():
            pass  # already there
        elif src and Path(src).exists():
            shutil.copy2(src, dest)
            print(f"  Copied {Path(src).name} -> {dest.name}")
        else:
            if src:
                print(f"  WARNING: {Path(src).name} not found, creating empty stub")
            empty_jar(dest)

    ivy_content = make_ivy_xml(org, module, revision, all_confs, ivy_pubs)
    ivy_file = module_dir / f"ivy-{revision}.xml"
    with open(ivy_file, 'w') as f:
        f.write(ivy_content)


def main():
    print(f"Setting up local Ivy repository at: {REPO_DIR}")

    # --------------- Simple 1-JAR modules ---------------

    setup_module("com.caucho", "com.caucho", "3.0.13", [
        {"name": "com.caucho", "src": LIB_DIR / "com.caucho-3.0.13.jar"},
    ])

    # Guava: use 15.0 JAR for 18.0 (backwards-compatible enough for compile)
    setup_module("com.google", "com.google.guava", "18.0", [
        {"name": "com.google.guava", "src": LIB_DIR / "com.google.guava-15.0.jar"},
    ])

    # Quartz: artifact name differs from module name
    setup_module("com.opensymphony.quartz", "org.quartz", "1.6.6", [
        {"name": "org.quartz", "src": LIB_DIR / "org.quartz-1.6.6.jar"},
    ])

    setup_module("com.opensymphony.sitemesh", "com.opensymphony.sitemesh", "2.2.1", [
        {"name": "com.opensymphony.sitemesh", "src": LIB_DIR / "com.opensymphony.sitemesh-2.2.1.jar"},
    ])

    setup_module("com.opensymphony.webwork", "com.opensymphony.webwork", "2.2.6", [
        {"name": "com.opensymphony.webwork", "src": LIB_DIR / "com.opensymphony.webwork-2.2.6.jar"},
    ])

    setup_module("com.opensymphony.xwork", "com.opensymphony.xwork", "1.2.3", [
        {"name": "com.opensymphony.xwork", "src": LIB_DIR / "com.opensymphony.xwork-1.2.3.jar"},
    ])

    # j2ssh: two JARs in one module
    setup_module("com.sshtools", "com.sshtools.j2ssh", "0.2.7", [
        {"name": "com.sshtools.j2ssh.common", "src": LIB_DIR / "com.sshtools.j2ssh.common-0.2.7.jar"},
        {"name": "com.sshtools.j2ssh.core",   "src": LIB_DIR / "com.sshtools.j2ssh.core-0.2.7.jar"},
    ])

    setup_module("com.sun.syndication", "com.sun.syndication", "0.8.0", [
        {"name": "com.sun.syndication", "src": LIB_DIR / "com.sun.syndication-0.8.0.jar"},
    ])

    setup_module("com.sun.syndication", "com.sun.syndication.content", "20060508", [
        {"name": "com.sun.syndication.content", "src": LIB_DIR / "com.sun.syndication.content-20060508.jar"},
    ])

    setup_module("com.thoughtworks.xstream", "com.thoughtworks.xstream", "1.4.2", [
        {"name": "com.thoughtworks.xstream", "src": LIB_DIR / "com.thoughtworks.xstream-1.4.2.jar"},
    ])

    setup_module("com.uwyn", "com.uwyn.jhighlight", "1.0.0", [
        {"name": "com.uwyn.jhighlight", "src": LIB_DIR / "com.uwyn.jhighlight-1.0.0.jar"},
    ])

    # --------------- Eclipse / OSGi ---------------

    # equinox: main OSGi runtime
    setup_module("eclipse", "equinox", "3.3", [
        {"name": "org.eclipse.osgi",
         "src": LIB_DIR / "org.eclipse.osgi_3.3.0.v20070530-3.3.jar"},
    ])

    # equinox-bundles: registry, common, jobs (revision 3.3_2 used by core module)
    setup_module("eclipse", "equinox-bundles", "3.3_2", [
        {"name": "org.eclipse.equinox.registry",
         "src": PLUGINS_DIR / "org.eclipse.equinox.registry_3.3.0.v20070522-3.3.jar"},
        {"name": "org.eclipse.equinox.common",
         "src": PLUGINS_DIR / "org.eclipse.equinox.common_3.3.0.v20070426-3.3.jar"},
        {"name": "org.eclipse.core.jobs",
         "src": PLUGINS_DIR / "org.eclipse.core.jobs_3.3.0.v20070423-3.3.jar"},
    ])

    # equinox-bundles revision 3.3 (used in packaging only)
    setup_module("eclipse", "equinox-bundles", "3.3", [
        {"name": "org.eclipse.equinox.registry",
         "src": PLUGINS_DIR / "org.eclipse.equinox.registry_3.3.0.v20070522-3.3.jar",
         "conf": ["default", "equinox-bundles"]},
        {"name": "org.eclipse.equinox.common",
         "src": PLUGINS_DIR / "org.eclipse.equinox.common_3.3.0.v20070426-3.3.jar",
         "conf": ["default", "equinox-bundles"]},
        {"name": "org.eclipse.core.jobs",
         "src": PLUGINS_DIR / "org.eclipse.core.jobs_3.3.0.v20070423-3.3.jar",
         "conf": ["default", "equinox-bundles"]},
    ], extra_confs=["equinox-bundles"])

    # --------------- javax ---------------

    setup_module("javax.mail", "javax.mail", "1.4.0", [
        {"name": "javax.mail", "src": LIB_DIR / "javax.mail-1.4.0.jar"},
    ])

    setup_module("javax.portlet", "javax.portlet", "2.0.0", [
        {"name": "javax.portlet", "src": LIB_DIR / "javax.portlet-2.0.0.jar"},
    ])

    # javax.script is built into JDK 6+; empty stub satisfies Ivy resolve
    setup_module("javax.script", "javax.script.api", "1.0.0", [
        {"name": "javax.script.api", "src": None},
    ])

    setup_module("javax.servlet", "javax.servlet", "2.4.0", [
        {"name": "javax.servlet", "src": LIB_DIR / "javax.servlet-2.4.0.jar"},
    ])

    setup_module("javax.transaction", "javax.transaction", "1.1.0", [
        {"name": "javax.transaction", "src": LIB_DIR / "javax.transaction-1.1.0.jar"},
    ])

    # --------------- net.sourceforge ---------------

    # cglib: artifact name is net.sf.cglib-nodep
    setup_module("net.sourceforge.cglib", "net.sf.cglib", "2.1.3", [
        {"name": "net.sf.cglib-nodep", "src": LIB_DIR / "net.sf.cglib-nodep-2.1.3.jar"},
    ])

    setup_module("net.sourceforge.ehcache", "net.sf.ehcache", "1.6.2", [
        {"name": "net.sf.ehcache", "src": LIB_DIR / "net.sf.ehcache-1.6.2.jar"},
    ])

    setup_module("net.sourceforge.flexjson", "net.sf.flexjson", "1.6.0", [
        {"name": "net.sf.flexjson", "src": LIB_DIR / "net.sf.flexjson-1.6.0.jar"},
    ])

    setup_module("nu.xom", "nu.xom", "20111125", [
        {"name": "nu.xom", "src": LIB_DIR / "nu.xom-20111125.jar"},
    ])

    # --------------- org.apache ---------------

    # ant: directoryscanner is a special configuration/artifact
    setup_module("org.apache.ant", "org.apache.ant", "1.6.5", [
        {"name": "org.apache.ant-directoryscanner",
         "src": LIB_DIR / "org.apache.ant-directoryscanner-1.6.5.jar",
         "conf": ["default", "directoryscanner"]},
    ], extra_confs=["directoryscanner"])

    # ivy: core and httpclient are special configurations
    setup_module("org.apache.ant", "org.apache.ivy", "2.0.0", [
        {"name": "org.apache.ivy",
         "src": LIB_DIR / "org.apache.ivy-2.0.0.jar",
         "conf": ["default", "core", "httpclient"]},
    ], extra_confs=["core", "httpclient"])

    setup_module("org.apache.commons", "org.apache.commons.cli", "1.0.0", [
        {"name": "org.apache.commons.cli", "src": LIB_DIR / "org.apache.commons.cli-1.0.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.collections", "3.2.0", [
        {"name": "org.apache.commons.collections", "src": LIB_DIR / "org.apache.commons.collections-3.2.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.dbcp", "1.3.0", [
        {"name": "org.apache.commons.dbcp", "src": LIB_DIR / "org.apache.commons.dbcp-1.3.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.fileupload", "1.2.2", [
        {"name": "org.apache.commons.fileupload", "src": LIB_DIR / "org.apache.commons.fileupload-1.2.2.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.httpclient", "3.1.0", [
        {"name": "org.apache.commons.httpclient", "src": LIB_DIR / "org.apache.commons.httpclient-3.1.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.io", "1.4.0", [
        {"name": "org.apache.commons.io", "src": LIB_DIR / "org.apache.commons.io-1.4.0.jar"},
    ])

    # commons.lang: use 2.5.0 JAR for requested 2.1.0
    setup_module("org.apache.commons", "org.apache.commons.lang", "2.1.0", [
        {"name": "org.apache.commons.lang", "src": LIB_DIR / "org.apache.commons.lang-2.5.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.pool", "1.2.0", [
        {"name": "org.apache.commons.pool", "src": LIB_DIR / "org.apache.commons.pool-1.2.0.jar"},
    ])

    setup_module("org.apache.commons", "org.apache.commons.vfs", "20100409", [
        {"name": "org.apache.commons.vfs", "src": LIB_DIR / "org.apache.commons.vfs-20100409.jar"},
    ])

    # directory server: test-only dep, empty stub
    setup_module("org.apache.directory", "org.apache.directory.server", "1.5.7", [
        {"name": "org.apache.directory.server", "src": None},
    ])

    setup_module("org.apache.velocity", "org.apache.velocity", "1.3.1", [
        {"name": "org.apache.velocity", "src": LIB_DIR / "org.apache.velocity-1.3.1.jar"},
    ])

    setup_module("org.apache.velocity", "org.apache.velocity.tools.views", "1.1.0", [
        {"name": "org.apache.velocity.tools.views", "src": LIB_DIR / "org.apache.velocity.tools.views-1.1.0.jar"},
    ])

    setup_module("org.apache.xmlrpc", "org.apache.xmlrpc", "2.0.1", [
        {"name": "org.apache.xmlrpc", "src": LIB_DIR / "org.apache.xmlrpc-2.0.1.jar"},
    ])

    # --------------- org.hibernate ---------------

    setup_module("org.hibernate", "org.hibernate", "3.3.1.GA", [
        {"name": "org.hibernate", "src": LIB_DIR / "org.hibernate-3.3.1.GA.jar"},
    ])

    setup_module("org.hsqldb", "org.hsqldb", "1.8.0.10", [
        {"name": "org.hsqldb", "src": LIB_DIR / "org.hsqldb-1.8.0.10.jar"},
    ])

    # --------------- org.igniterealtime ---------------

    # smack: two JARs
    setup_module("org.igniterealtime", "org.jivesoftware.smackx", "2.2.1", [
        {"name": "org.jivesoftware.smack",  "src": LIB_DIR / "org.jivesoftware.smack-2.2.1.jar"},
        {"name": "org.jivesoftware.smackx", "src": LIB_DIR / "org.jivesoftware.smackx-2.2.1.jar"},
    ])

    # jfree: two JARs
    setup_module("org.jfree.chart", "org.jfree.chart", "1.0.13", [
        {"name": "org.jfree.chart",   "src": LIB_DIR / "org.jfree.chart-1.0.13.jar"},
        {"name": "org.jfree.jcommon", "src": LIB_DIR / "org.jfree.jcommon-1.0.16.jar"},
    ])

    setup_module("org.json", "org.json", "1.0.0", [
        {"name": "org.json", "src": LIB_DIR / "org.json-1.0.0.jar"},
    ])

    # junit and mockito: test-only — try downloading from Maven Central
    junit_jar = REPO_DIR / "org.junit/org.junit/jars/org.junit-4.8.2.jar"
    junit_jar.parent.mkdir(parents=True, exist_ok=True)
    if not junit_jar.exists():
        ok = download(f"{MAVEN_CENTRAL}/junit/junit/4.8.2/junit-4.8.2.jar", junit_jar)
        if not ok:
            empty_jar(junit_jar)
    setup_module("org.junit", "org.junit", "4.8.2", [
        {"name": "org.junit", "src": junit_jar},
    ])

    mockito_jar = REPO_DIR / "org.mockito/org.mockito/jars/org.mockito-1.5.0.jar"
    mockito_jar.parent.mkdir(parents=True, exist_ok=True)
    if not mockito_jar.exists():
        ok = download(
            f"{MAVEN_CENTRAL}/org/mockito/mockito-all/1.5/mockito-all-1.5.jar",
            mockito_jar,
        )
        if not ok:
            # try alternate coordinates
            ok = download(
                f"{MAVEN_CENTRAL}/org/mockito/mockito-core/1.5/mockito-core-1.5.jar",
                mockito_jar,
            )
        if not ok:
            empty_jar(mockito_jar)
    setup_module("org.mockito", "org.mockito", "1.5.0", [
        {"name": "org.mockito", "src": mockito_jar},
    ])

    # jetty: use 5.1.14 for 5.1.15
    setup_module("org.mortbay.jetty", "org.mortbay.jetty", "5.1.15", [
        {"name": "org.mortbay.jetty", "src": LIB_DIR / "org.mortbay.jetty-5.1.14.jar"},
    ])

    # netbeans cvsclient: not on Maven Central; empty stub (CVS bundle excluded from build)
    setup_module("org.netbeans", "org.netbeans.lib.cvsclient", "5.5", [
        {"name": "org.netbeans.lib.cvsclient", "src": None},
    ])

    # jython: acceptance test dep (acceptance skipped); empty stub
    setup_module("org.python", "org.jython", "2.5.2", [
        {"name": "org.jython", "src": None},
    ])

    # --------------- org.slf4j ---------------

    # slf4j: jdk conf needs both api and jdk14 JARs
    setup_module("org.slf4j", "org.slf4j", "1.5.3", [
        {"name": "org.slf4j-api",   "src": LIB_DIR / "org.slf4j-api-1.5.3.jar",
         "conf": ["default", "jdk"]},
        {"name": "org.slf4j-jdk14", "src": LIB_DIR / "org.slf4j-jdk14-1.5.3.jar",
         "conf": ["default", "jdk"]},
    ], extra_confs=["jdk"])

    # --------------- org.springframework ---------------

    for mod in ["org.springframework.aop", "org.springframework.beans",
                "org.springframework.context", "org.springframework.context.support",
                "org.springframework.jdbc", "org.springframework.orm",
                "org.springframework.transaction", "org.springframework.web"]:
        setup_module("org.springframework", mod, "3.0.5.RELEASE", [
            {"name": mod, "src": LIB_DIR / f"{mod}-3.0.5.RELEASE.jar"},
        ])

    setup_module("org.springframework", "org.springframework.ldap", "1.3.0.RELEASE", [
        {"name": "org.springframework.ldap", "src": LIB_DIR / "org.springframework.ldap-1.3.0.RELEASE.jar"},
    ])

    # spring.core: aspectj conf adds weaver + asm
    setup_module("org.springframework", "org.springframework.core", "3.0.5.RELEASE", [
        {"name": "org.springframework.core",
         "src": LIB_DIR / "org.springframework.core-3.0.5.RELEASE.jar",
         "conf": ["default", "aspectj"]},
        {"name": "org.springframework.asm",
         "src": LIB_DIR / "org.springframework.asm-3.0.5.RELEASE.jar",
         "conf": ["default", "aspectj"]},
        {"name": "org.aspectj.weaver",
         "src": LIB_DIR / "org.aspectj.weaver-1.6.8.RELEASE.jar",
         "conf": ["aspectj"]},
    ], extra_confs=["aspectj"])

    for mod in ["org.springframework.security.acls", "org.springframework.security.config",
                "org.springframework.security.core", "org.springframework.security.ldap",
                "org.springframework.security.web"]:
        setup_module("org.springframework.security", mod, "3.0.5.RELEASE", [
            {"name": mod, "src": LIB_DIR / f"{mod}-3.0.5.RELEASE.jar"},
        ])

    # --------------- sun ---------------

    # jna: use 3.3.0 for 4.1.0 (close enough for compile; may need download)
    jna_jar = REPO_DIR / "sun/jna/jars/jna-4.1.0.jar"
    jna_jar.parent.mkdir(parents=True, exist_ok=True)
    if not jna_jar.exists():
        ok = download(f"{MAVEN_CENTRAL}/net/java/dev/jna/jna/4.1.0/jna-4.1.0.jar", jna_jar)
        if not ok:
            shutil.copy2(LIB_DIR / "jna-3.3.0.jar", jna_jar)
            print(f"  Fell back to jna-3.3.0.jar for jna-4.1.0.jar")
    setup_module("sun", "jna", "4.1.0", [
        {"name": "jna", "src": jna_jar},
    ])

    # sjsxp: two JARs
    setup_module("sun", "sjsxp", "1.0.1", [
        {"name": "sjsxp",            "src": LIB_DIR / "sjsxp-1.0.1.jar"},
        {"name": "sjsxp-jsr173-api", "src": LIB_DIR / "sjsxp-jsr173-api-1.0.1.jar"},
    ])

    # --------------- thoughtworks ---------------

    setup_module("thoughtworks", "javasysmon", "0.3.4", [
        {"name": "javasysmon", "src": LIB_DIR / "javasysmon-0.3.4.jar"},
    ])

    # --------------- svnkit (for SVN bundle) ---------------

    svnkit_186_jar = REPO_DIR / "com.svnkit/com.svnkit-1.8.6/jars/com.svnkit-1.8.6.jar"
    svnkit_186_jar.parent.mkdir(parents=True, exist_ok=True)
    if not svnkit_186_jar.exists():
        ok = download(
            f"{MAVEN_CENTRAL}/org/tmatesoft/svnkit/svnkit/1.8.6/svnkit-1.8.6.jar",
            svnkit_186_jar,
        )
        if not ok:
            empty_jar(svnkit_186_jar)
    setup_module("com.svnkit", "com.svnkit", "1.8.6", [
        {"name": "com.svnkit", "src": svnkit_186_jar},
    ])

    # svnkit 1.7.11 (acceptance tests, acceptance is skipped)
    svnkit_1711_jar = REPO_DIR / "com.svnkit/com.svnkit-1.7.11/jars/com.svnkit-1.7.11.jar"
    svnkit_1711_jar.parent.mkdir(parents=True, exist_ok=True)
    if not svnkit_1711_jar.exists():
        ok = download(
            f"{MAVEN_CENTRAL}/org/tmatesoft/svnkit/svnkit/1.7.11/svnkit-1.7.11.jar",
            svnkit_1711_jar,
        )
        if not ok:
            empty_jar(svnkit_1711_jar)
    setup_module("com.svnkit", "com.svnkit", "1.7.11", [
        {"name": "com.svnkit", "src": svnkit_1711_jar},
    ])

    # --------------- acceptance test stubs ---------------

    for (org, mod, rev) in [
        ("com.dumbster",           "com.dumbster.smtp",        "20090217"),
        ("com.thoughtworks.selenium", "com.thoughtworks.selenium", "2.44.0"),
    ]:
        setup_module(org, mod, rev, [{"name": mod, "src": None}])

    # --------------- zutubi non-source modules ---------------

    # boot: zutubi bootstrap JAR
    setup_module("zutubi", "boot", "2.1", [
        {"name": "boot", "src": BOOT_JAR},
    ])

    # diff and events: pre-built Zutubi libraries
    setup_module("zutubi", "diff", "3.0.3", [
        {"name": "com.zutubi.diff", "src": LIB_DIR / "com.zutubi.diff-3.0.3.jar"},
    ])

    setup_module("zutubi", "events", "3.0.2", [
        {"name": "com.zutubi.events", "src": LIB_DIR / "com.zutubi.events-3.0.2.jar"},
    ])

    print("\n=== Local repository setup complete ===")
    print(f"Repository at: {REPO_DIR}")


if __name__ == "__main__":
    main()
