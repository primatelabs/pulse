#!/usr/bin/env python3
"""
Add missing transitive dependencies to the local Ivy repository.
Run after setup-local-repo.py to fill in transitive dep gaps.
"""

import shutil
import zipfile
from pathlib import Path

LIB_DIR = Path.home() / "Scratch/pulse-server/pulse-2.6.24/versions/0206024000/lib"
REPO_DIR = Path(__file__).parent / "repository"


def empty_jar(path):
    with zipfile.ZipFile(path, 'w') as _:
        pass


def make_simple_module(org, module, rev, artifact_name, src_jar, extra_confs=None):
    """Create a minimal module with one artifact."""
    d = REPO_DIR / org / module
    jars = d / "jars"
    jars.mkdir(parents=True, exist_ok=True)

    dest = jars / f"{artifact_name}-{rev}.jar"
    if not dest.exists():
        src = Path(src_jar)
        if src.exists():
            shutil.copy2(src, dest)
            print(f"  Copied {src.name} -> {dest.name}")
        else:
            empty_jar(dest)
            print(f"  Created stub: {dest.name}")

    confs = set(extra_confs or [])
    confs.add('default')
    conf_xml = '\n'.join(f'        <conf name="{c}" visibility="public"/>' for c in sorted(confs))

    ivy = d / f"ivy-{rev}.xml"
    ivy.write_text(f'''<?xml version="1.0" encoding="UTF-8"?>
<ivy-module version="2.0">
    <info organisation="{org}" module="{module}" revision="{rev}" status="release"/>
    <configurations>
{conf_xml}
    </configurations>
    <publications>
        <artifact name="{artifact_name}" type="jar" ext="jar" conf="default"/>
    </publications>
</ivy-module>
''')


def update_ivy_with_deps(org, module, rev, extra_pubs=None, deps=None):
    """
    Rewrite an existing module's ivy.xml to add extra publications and/or dependencies.
    extra_pubs: list of (artifact_name, src_jar, confs) for additional artifacts
    deps: list of (dep_org, dep_module, dep_rev, dep_conf) for transitive deps
    """
    d = REPO_DIR / org / module
    jars = d / "jars"
    jars.mkdir(parents=True, exist_ok=True)
    ivy_file = d / f"ivy-{rev}.xml"

    # Read existing content
    content = ivy_file.read_text() if ivy_file.exists() else ""

    # Parse existing confs and publications from content (simple approach: regenerate)
    # For simplicity, just regenerate the entire file

    # Collect existing artifacts from jars dir
    existing_artifacts = []
    for jar in sorted(jars.iterdir()):
        if jar.name.endswith('.jar') and jar.name.endswith(f'-{rev}.jar'):
            art_name = jar.name[:-len(f'-{rev}.jar')]
            existing_artifacts.append(art_name)

    # Add new artifacts
    for (art_name, src_jar, art_confs) in (extra_pubs or []):
        dest = jars / f"{art_name}-{rev}.jar"
        if not dest.exists():
            src = Path(src_jar) if src_jar else None
            if src and src.exists():
                shutil.copy2(src, dest)
                print(f"  Added artifact {art_name}-{rev}.jar")
            else:
                empty_jar(dest)
                print(f"  Added stub artifact {art_name}-{rev}.jar")
        if art_name not in existing_artifacts:
            existing_artifacts.append(art_name)

    all_confs = {'default'}
    for (art_name, src_jar, art_confs) in (extra_pubs or []):
        for c in art_confs:
            all_confs.add(c)
    for dep in (deps or []):
        pass  # deps don't add confs

    conf_xml = '\n'.join(f'        <conf name="{c}" visibility="public"/>'
                          for c in sorted(all_confs))

    pub_xml = '\n'.join(
        f'        <artifact name="{a}" type="jar" ext="jar" conf="default"/>'
        for a in existing_artifacts
    )

    dep_xml = ''
    if deps:
        dep_lines = []
        for (dep_org, dep_mod, dep_rev, dep_conf) in deps:
            dep_lines.append(
                f'        <dependency org="{dep_org}" name="{dep_mod}" '
                f'rev="{dep_rev}" conf="{dep_conf}" transitive="false"/>'
            )
        dep_xml = f'''
    <dependencies>
{chr(10).join(dep_lines)}
    </dependencies>'''

    ivy_file.write_text(f'''<?xml version="1.0" encoding="UTF-8"?>
<ivy-module version="2.0">
    <info organisation="{org}" module="{module}" revision="{rev}" status="release"/>
    <configurations>
{conf_xml}
    </configurations>
    <publications>
{pub_xml}
    </publications>{dep_xml}
</ivy-module>
''')
    print(f"  Updated ivy.xml: {org}/{module}:{rev}")


# ── New standalone modules ────────────────────────────────────────────────────

print("=== Adding missing transitive dependency modules ===\n")

make_simple_module("org.apache.commons", "org.apache.commons.codec",   "1.3.0",
                   "org.apache.commons.codec",   LIB_DIR / "org.apache.commons.codec-1.3.0.jar")

make_simple_module("org.apache.commons", "org.apache.commons.logging", "1.2",
                   "org.apache.commons.logging", LIB_DIR / "org.apache.commons.logging-1.2.jar")

make_simple_module("org.apache.commons", "org.apache.commons.beanutils","1.7.0",
                   "org.apache.commons.beanutils",LIB_DIR / "org.apache.commons.beanutils-1.7.0.jar")

make_simple_module("org.ognl", "org.ognl", "2.6.11",
                   "org.ognl", LIB_DIR / "org.ognl-2.6.11.jar")

make_simple_module("com.opensymphony", "com.opensymphony.util", "2.2.5",
                   "com.opensymphony.util", LIB_DIR / "com.opensymphony.util-2.2.5.jar")

make_simple_module("org.antlr", "org.antlr", "2.7.6",
                   "org.antlr", LIB_DIR / "org.antlr-2.7.6.jar")

make_simple_module("org.dom4j", "org.dom4j", "1.6.1",
                   "org.dom4j", LIB_DIR / "org.dom4j-1.6.1.jar")

make_simple_module("org.jboss.javassist", "org.jboss.javassist", "3.3.0.ga",
                   "org.jboss.javassist", LIB_DIR / "org.jboss.javassist-3.3.0.ga.jar")

make_simple_module("org.aopalliance", "org.aopalliance", "1.0.0",
                   "org.aopalliance", LIB_DIR / "org.aopalliance-1.0.0.jar")

make_simple_module("org.freemarker", "org.freemarker", "2.3.12",
                   "org.freemarker", LIB_DIR / "org.freemarker-2.3.12.jar")

make_simple_module("org.apache.ws.commons", "org.apache.ws.commons", "1.0.0",
                   "org.apache.ws.commons", LIB_DIR / "org.apache.ws.commons-1.0.0.jar")

make_simple_module("org.jdom", "org.jdom", "1.0.0",
                   "org.jdom", LIB_DIR / "org.jdom-1.0.0.jar")

make_simple_module("com.uwyn", "com.uwyn.rife.continuations", "0.0.1",
                   "com.uwyn.rife.continuations", LIB_DIR / "com.uwyn.rife.continuations-0.0.1.jar")

make_simple_module("javax.activation", "javax.activation", "1.1.0",
                   "javax.activation", LIB_DIR / "javax.activation-1.1.0.jar")

# jsdeps: Pulse JavaScript dependency tool
make_simple_module("jsdeps", "jsdeps", "0.2",
                   "jsdeps", LIB_DIR / "jsdeps-0.2.jar")

# Spring modules not yet added
make_simple_module("org.springframework", "org.springframework.expression", "3.0.5.RELEASE",
                   "org.springframework.expression",
                   LIB_DIR / "org.springframework.expression-3.0.5.RELEASE.jar")

make_simple_module("org.springframework", "org.springframework.oxm", "3.0.5.RELEASE",
                   "org.springframework.oxm",
                   LIB_DIR / "org.springframework.oxm-3.0.5.RELEASE.jar")

# velocity-dep: velocity with all dependencies bundled (some modules use it)
make_simple_module("org.apache.velocity", "org.apache.velocity.dep", "1.3.1",
                   "org.apache.velocity-dep", LIB_DIR / "org.apache.velocity-dep-1.3.1.jar")

# ── Add jna-platform as additional artifact of sun/jna ────────────────────────

print("\n=== Updating existing modules with transitive deps ===\n")

update_ivy_with_deps("sun", "jna", "4.1.0",
    extra_pubs=[
        ("jna-platform", LIB_DIR / "jna-platform-3.3.0.jar", ["default"]),
    ]
)

# ── Update parent modules to declare transitive deps ─────────────────────────

# com.opensymphony.xwork depends on ognl, util, aopalliance, commons-lang
update_ivy_with_deps("com.opensymphony.xwork", "com.opensymphony.xwork", "1.2.3",
    deps=[
        ("org.ognl",           "org.ognl",            "2.6.11",   "default->default"),
        ("com.opensymphony",   "com.opensymphony.util","2.2.5",    "default->default"),
        ("org.aopalliance",    "org.aopalliance",     "1.0.0",    "default->default"),
        ("org.apache.commons", "org.apache.commons.lang", "2.1.0","default->default"),
        ("org.freemarker",     "org.freemarker",      "2.3.12",   "default->default"),
    ]
)

# com.opensymphony.webwork depends on rife.continuations
update_ivy_with_deps("com.opensymphony.webwork", "com.opensymphony.webwork", "2.2.6",
    deps=[
        ("com.uwyn",           "com.uwyn.rife.continuations", "0.0.1", "default->default"),
    ]
)

# commons.httpclient depends on codec and logging
update_ivy_with_deps("org.apache.commons", "org.apache.commons.httpclient", "3.1.0",
    deps=[
        ("org.apache.commons", "org.apache.commons.codec",   "1.3.0", "default->default"),
        ("org.apache.commons", "org.apache.commons.logging", "1.2",   "default->default"),
    ]
)

# hibernate depends on antlr, dom4j, javassist, aopalliance, slf4j
update_ivy_with_deps("org.hibernate", "org.hibernate", "3.3.1.GA",
    deps=[
        ("org.antlr",          "org.antlr",           "2.7.6",    "default->default"),
        ("org.dom4j",          "org.dom4j",           "1.6.1",    "default->default"),
        ("org.jboss.javassist","org.jboss.javassist", "3.3.0.ga", "default->default"),
        ("org.aopalliance",    "org.aopalliance",     "1.0.0",    "default->default"),
        ("org.slf4j",          "org.slf4j",           "1.5.3",    "default->default"),
        ("org.apache.commons", "org.apache.commons.collections", "3.2.0", "default->default"),
    ]
)

# xmlrpc depends on ws.commons
update_ivy_with_deps("org.apache.xmlrpc", "org.apache.xmlrpc", "2.0.1",
    deps=[
        ("org.apache.ws.commons", "org.apache.ws.commons", "1.0.0", "default->default"),
    ]
)

# spring aop depends on aopalliance
update_ivy_with_deps("org.springframework", "org.springframework.aop", "3.0.5.RELEASE",
    deps=[
        ("org.aopalliance", "org.aopalliance", "1.0.0", "default->default"),
    ]
)

# javax.mail depends on activation
update_ivy_with_deps("javax.mail", "javax.mail", "1.4.0",
    deps=[
        ("javax.activation", "javax.activation", "1.1.0", "default->default"),
    ]
)

# xstream depends on xpp3 / xmlpull (but we may not have those; skip for now)

# velocity depends on commons-lang, commons-collections, commons-logging
update_ivy_with_deps("org.apache.velocity", "org.apache.velocity", "1.3.1",
    deps=[
        ("org.apache.commons", "org.apache.commons.lang",        "2.1.0", "default->default"),
        ("org.apache.commons", "org.apache.commons.collections", "3.2.0", "default->default"),
        ("org.apache.commons", "org.apache.commons.logging",     "1.2",   "default->default"),
        ("org.jdom",           "org.jdom",                       "1.0.0", "default->default"),
    ]
)

# velocity tools depends on commons-beanutils, commons-lang
update_ivy_with_deps("org.apache.velocity", "org.apache.velocity.tools.views", "1.1.0",
    deps=[
        ("org.apache.commons", "org.apache.commons.beanutils", "1.7.0", "default->default"),
        ("org.apache.commons", "org.apache.commons.lang",      "2.1.0", "default->default"),
    ]
)

# smack depends on logging
update_ivy_with_deps("org.igniterealtime", "org.jivesoftware.smackx", "2.2.1",
    deps=[
        ("org.apache.commons", "org.apache.commons.logging", "1.2", "default->default"),
    ]
)

print("\n=== Done ===")
