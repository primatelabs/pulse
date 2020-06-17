# Pulse

Pulse is a continuous integration server.

## Building

- All builds require access to the zutubi Ivy repository, but that should be open to you.
- There is an Apache Ant build in the top directory which should work with recent Ant versions (definitely 1.8.x).
- To build everything and run all unit tests, run:

`ant build.all`

Actually I expect tests to fail without other dependencies installed, although it may run through to the plugins (it's common to first fail on Maven plugin tests that need Maven installed with env variables pointing at it).  You can instead run:

`ant -Dskip.tests=true build.all`

To build/run from an IDE we have some support for IntelliJ (free version is fine).  You can try running:

`ant setup.dev`

to generate IntelliJ modules for all plugins, then open the project (pulse.ipr/iws are generated in the root directory from templates in the etc/ directory).  This should even include a run configuration to start Pulse from the IDE.  I haven't tested from scratch lately, though!

To build a dev package, run:

`ant -Dskip.tests=true build.all package.master`

This should create a Pulse tarball/zip with dev versions under the build/ directory.  In fact on Windows with NSIS available it also creates a .exe installer.

To build a versioned/release tarball, run:

`./prepare-release.sh 3.0.12`

This configures versions in various files (avoid committing these!) , so when you run:

`ant -Dskip.tests=true build.all package.master`

You get a release tarball in build/.
