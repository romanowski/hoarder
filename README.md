# Hoarder - don't repeat yourself when compiling!

Hoarder is the set of sbt plugins (for sbt 1.0.x and 0.13.x) that allows you to reuse compilation data from other workspaces (aka. _Cached compilation_).

Hoarder is based on zinc (from [this PR](https://github.com/romanowski/zinc/pull/2)). 

The idea is simple: your code is compiled many times (e.g. during CI builds) and results are usually trashed. Instead, it's better to distribute classfiles (together with the cached compilation metadata) and don't repeat this work again and again.

Using caches will not make your local workspace read-only. Once a cache is imported, it will work exactly as locally compiled one (all following compilations will be normal, incremental ones).


## Getting started

In order to use Hoarder in your project just add it as a standard sbt plugin (for a given project or globally):

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.github.romanowski" % "hoarder" % "1.0-SNAPSHOT")
```

Hoarder does not have any stable release so far, and you can track progress for sbt [0.13.x](https://github.com/romanowski/hoarder/milestone/1) and [1.0.x](https://github.com/romanowski/hoarder/milestone/2)

## Integration with your project

Hoarder can be used in your project in multiple ways described below. For now only the 'stash' workflow is supported, but there's more to come (feel free to create an issue with your own ideas).

### Stash

Stash workflow allows you to stash compilation results similarly to changes in git.
Running `stash` task will store your current compilation data in a global directory for your project and later you can import that compilation using `stashApply` command. More can be found in [docs](docs/stash.md).

### From release

Not implemented yet. See [#2](https://github.com/romanowski/hoarder/issues/2) for more details.


## Cached compilation. How does it work?

[Zinc incremental compiler](https://github.com/sbt/zinc/) (previously part of sbt) beside classfiles generates incremental compilation metadata that allows it later to recompile only the subset of classfiles. In [this PR](https://github.com/romanowski/zinc/pull/2) zinc was able to export that metadata in format that can be reused.

 


