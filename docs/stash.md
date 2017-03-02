# Stash workflow

The stash workflow allows you to stash the compilation results similarly to changes in git. Running stash task will store your current compilation data in a global directory for your project and later you can import that compilation using stashApply command. 

## How does it work

Stash/StashApply is a really simple workflow. Similarly to `git stash` you can save state of current compilation in global (common for all projects) cache. Later on if you decide that you want to use it you load them.

### Gobal cache

Hoarder stores caches in global directory that is configured in `globalStashLocation` setting which by default points to `<global-sbt-dir>/"sbt-stash"`(what is is translated to `~/.sbt/0.13/sbt-stash` on my machine).

In order to store results from multiple compilaitons from multiple sbt projects, each cache is described by project and version labels.

Both stash and stashApply accept two optional parameters that control project label and version label but both got settings for default values:
* `defaultProjectLabel` that is by default set to the name ot root directory where your project lives
* `defaultVersionLabel` that is set to 'HEAD' by default

### Stash task

`stash [<projectLabe>l [<versionLabel>]]`

This task will stash compilation artifacts for provided project (or for all if non is provided) and all its dependecies.

Stash command will run `compile` for your project and its tests so project needs to compile in order to be stashed.

By default you cannot override the exisiting cache but this behaviour can be changed by `overrideExistingCache` setting.

### StashApply task

`stashApply [<projectLabe>l [<versionLabel>]]`

This task will load stashed compilation artifacts for provided project (or for all if non is provided) and all its dependecies.

Loading cached results needs to clean up previous compilation (if exsist) and by default it will remove all *.class files from output. This behaviour can be chaneged with `cleanOutputMode` settings (currently FailOnNonEmpty, CleanOutput and CleanClasses are supported)
