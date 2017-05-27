# Hoarder - don't repeat yourself when compiling!

[![Build Status](https://api.travis-ci.org/romanowski/hoarder.png?branch=master)](https://travis-ci.org/romanowski/hoarder)
[![Gitter room](https://badges.gitter.im/sbt_hoarder/Lobby.svg)](https://gitter.im/sbt_hoarder/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Current realease](https://maven-badges.herokuapp.com/maven-central/com.github.romanowski/hoarder/badge.png)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.romanowski%22)

Hoarder is the set of sbt plugins (for sbt 1.0.x and 0.13.x) that allows you to reuse compilation data from other workspaces (aka. _Cached compilation_).

Hoarder is based on zinc (from [this PR](https://github.com/romanowski/zinc/pull/2)). 

The idea is simple: your code is compiled many times (e.g. during CI builds) and results are usually trashed. Instead, it's better to distribute classfiles (together with the cached compilation metadata) and don't repeat this work again and again.

Using caches will not make your local workspace read-only. Once a cache is imported, it will work exactly as locally compiled one (all following compilations will be normal, incremental ones).


## Getting started

In order to use Hoarder in your project just add it as a standard sbt plugin (for a given project or globally):

```scala
addSbtPlugin("com.github.romanowski" % "hoarder" % "1.0.1-M2")
```

Hoarder does not have any stable release so far, and you can track progress for sbt [0.13.x](https://github.com/romanowski/hoarder/milestone/1) and [1.0.x](https://github.com/romanowski/hoarder/milestone/2)

## Integration with your project

Hoarder can be used in your project in multiple ways (workflows) described below. For now onlt few are supported but there's more to come (feel free to create issues with your own ideas).

### [Stash workflow](docs/stash.md)

Stash workflow allows you to stash compilation results similarly to changes in git.
Running `stash` task will store your current compilation data in a global directory for your project and later you can import that compilation using `stashApply` command. More can be found in [docs](docs/stash.md).

### [Cached PR verfication builds (e.g. Travis ones)](docs/stash.md)

This workflow main goal is speed up of PR verification builds incrementally recompiling changes in PR. 

There are 2 parts of this workflow: *PR verification build* (that use generated cached to speed up compilation) and *build after PR is merged* (that performs full compilaiton to be super safe and generate caches for future PR verification builds).

Hoarder allows anyone to define custom PR integration by providing new `CachedCI.Setup` instance.

Hoarder also provides predefine integrations e.g. for Travis. In order to use it (Travis one) all you need to do is:
 * create custom autoplugin with configuration in `project` that extends one of predefined flows e.g. 
 ```scala
// project/CachedCi.scala
import org.romanowski.hoarder.actions.ci.TravisPRValidation
 
object CachedCi extends TravisPRValidation.PluginBase 
```
 * add cache definition to your build definition (usually`.travis.yml` file)
```yaml
cache:
  directories:
  - .hoarder-cache
```
 * add hoarder `sbt preBuild` and `sbt postBuild` tasks around your build commands, e.g.
 ```yaml
 - sbt preBuild 
 - sbt <your-original-build-command>
 - sbt postBuild
 ```

Minimal example configuration for ensime-server can be found in [this PR](https://github.com/romanowski/ensime-server/pull/1).
 
More can be found in [docs](docs/prVerification.md).

### From release

Not implemented yet. See [#2](https://github.com/romanowski/hoarder/issues/2) for more details.


## Cached compilation. How does it work?

[Zinc incremental compiler](https://github.com/sbt/zinc/) (previously part of sbt) beside classfiles generates incremental compilation metadata that allows it later to recompile only the subset of classfiles. In [this PR](https://github.com/romanowski/zinc/pull/2) zinc was able to export that metadata in format that can be reused.

 


