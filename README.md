# Hoarder - don't repeat yourself compiling!

Hoarder is set of sbt plugins (for sbt 1.0.x and 0.13.x) that allows you to reuse compilation from other workspaces (aka. _Cached compilation_).

Hoarder is based on zinc (from [this PR](https://github.com/romanowski/zinc/pull/2)). 

The idea is simple: your code is compiled many times (e.g. on CIs) and results are usually thrashed. Instead, dist some (alongside with cached compilation metadata) and don't repeat this work again and again.

Using cache will not make your local workspace read-only, once cached is imported it will work exactly as locally compiled one (all following compilations will be normal, incremental ones).


## Getting started

TODO (based in integration belows)

## Integration with you project

Integration with hoarder comes in two parts: exporting and importing caches. So far it needs to be done manually (importCache/exportCache tasks) but in future it needs to be automated.

TODO add more integrations based on ideas. 

## Cached compilaiton. How does it work?

[Zinc incremental compier](https://github.com/sbt/zinc/)(previously part of sbt) beside classfiles generates incremental compilation metadata that allows it later to recompiler only subset of classfiles. In [this PR](https://github.com/romanowski/zinc/pull/2) zinc was able to export that metadata in format that can be reused.

TODO more detalis!

 


