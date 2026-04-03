# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pulse is a distributed continuous integration (CI) server built in Java. It uses a master-slave architecture where the master server orchestrates builds and slaves (agents) execute them on remote machines. The project is developed by Zutubi Pty Ltd.

## Build System

**Tool:** Apache Ant with Apache Ivy for dependency management.

```bash
# Full build with tests
ant build.all

# Build skipping tests (faster)
ant -Dskip.tests=true build.all

# Package the master server
ant -Dskip.tests=true build.all package.master

# Set up IntelliJ IDEA project files
ant setup.dev

# Clean everything
ant clean
```

## Running Tests

```bash
# Run all module tests (included in build.all)
ant build.all

# Run tests for a single module
cd com.zutubi.pulse.core
ant test

# Run acceptance tests against a packaged master
ant accept.master
```

Test reports are generated in `build/reports/junit/` (XML and HTML).

## Linting

```bash
# Lint JavaScript files in com.zutubi.pulse.master/src/www/js/
ant check.js
```

## Architecture

Modules are prefixed by package name and follow a strict dependency hierarchy:

```
com.zutubi.util                    # Base utilities (Guava, etc.)
  └─ com.zutubi.i18n               # Internationalization
  └─ com.zutubi.validation         # Validation framework
  └─ com.zutubi.tove               # Type-safe configuration framework (core of all config UI)
       └─ com.zutubi.pulse.core    # CI logic: recipes, commands, SCM, events, plugin infrastructure
            └─ com.zutubi.pulse.servercore  # Shared server logic (Jetty, servlets)
                 ├─ com.zutubi.pulse.master  # Master server: web UI, DB, scheduler, orchestration
                 └─ com.zutubi.pulse.slave   # Slave agent: executes recipes from master
```

**Additional modules:**
- `com.zutubi.pulse.dev` — Developer tools
- `com.zutubi.pulse.acceptance` — Acceptance/integration test suite
- `bundles/` — 40+ OSGi plugin bundles (build tool integrations: Ant, Maven, Make, MSBuild, etc.)

**Key technologies:**
- Web layer: WebWork 2.2.6 with Velocity/Freemarker templates (in `com.zutubi.pulse.master/src/www/`)
- ORM: Hibernate 3.3.1 with HSQLDB
- Plugin system: OSGi Equinox
- Scheduling: Quartz
- Servlet container: Jetty

**Tove framework** (`com.zutubi.tove`) is a custom configuration management system used throughout Pulse for type-safe, annotation-driven configuration objects. All configuration in the system flows through Tove.

Each module has its own `build.xml` and `ivy.xml`. The root `common-build.xml` provides shared targets (`build`, `test`, `package`, `publish`, `dist`) inherited by all modules.

## Running the Server

After packaging, start the master server with:

```bash
cd build/package/pulse-2.7.0-dev
bash ./bin/pulse start -p 8080
```

Then open `http://localhost:8080` — default credentials are `admin` / `admin`.

Use `-d <dir>` to set a custom data directory (default: `~/.pulse2`), which is useful to keep test data separate:

```bash
bash ./bin/pulse start -p 8080 -d /tmp/pulse-test-data
```

To stop: `bash ./bin/pulse shutdown`

## Local Build Configuration

Copy `etc/local.properties.template` to `local.properties` to override build settings locally (e.g., to use a local Ivy repository for offline builds).
