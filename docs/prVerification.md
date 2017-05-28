# Cached PR verification builds (e.g. Travis ones)

Cached PR verification workflow is designed to speed up PR verification builds on your CI by compiling changes incrementally rather then do a clean build every time.

There are 2 part of that flow (represented by 2 different kind of CI build):
 * **PR verification build** (pr build in Travis): this job consume caches and we generally want it to be as fast as possible (since we usually wait for it to finish).
 * **Build after PR is merged** (push build in Travis): This job is triggered after PR is merged. It will be always full build and it's goal is to **generate new caches** and verify if build is green.

When Hoarder is used it is essential to have post-merge builds since no incremental compilation can give **100% guarantee that all required sources are recompiled**.

**Using hoarder for PR verification may give some false green/red PRs** that is why it is important to keep an eye on post-merge build and in case of failure revert merge and fix problem.

## How does it work

In order to enable PR verification flow in your project all you need to do is:
 * Create your own CI configuration autoplugin. It only sound scary: all you need to do is to create object that extends from one of Hoarder's plugin bases.
 * Wrap your sbt calls on CI in `preBuild` and `postBuild` tasks.
 * Configure your caching (this depend on picked integration)

Minimal example configuration for ensime-server can be found in [this PR](https://github.com/romanowski/ensime-server/pull/2).

### Travis integration

Travis CI allow us to store build caches and this is what we will use to store hoarder caches.

By default caches will be stored in top-level directory called `.hoarder-cache` (however different directory can be specified in PluginBase constructor).

Of course you would need to specified cache directory in your travis build definition:

```yml
cache:
  directories:
    # Other caches
    - .hoarder-cache
```

Next you would need to wrap your sbt calls with `preBuild` and `postBuild` sbt tasks:

```yml
  - sbt preBuild <your-build-def> postBuild
```

Last step step is to create autoplugin that will configure your sbt project. This means that you will need to create scala object in your `project` directory that extends from Travis CI base. Example:
```scala
import org.romanowski.hoarder.actions.ci.TravisPRValidation

object CachedCi extends TravisPRValidation.PluginBase
```

That's all!

### Amazon S3 integration

See for more details #38 and please let me know if you are interested.

### FTP/SFTP integration

See for more details #28 and please let me know if you are interested.

### Custom integration

Creating custom integration is really simple. Simiarly to predefined flows you would need to create custom autoplugin but this time it need to extends from [CachedCI.PluginBase]((https://github.com/romanowski/hoarder/blob/master/0.13/hoarder/src/main/scala/org/romanowski/hoarder/actions/CachedCI.scala)) class.

I means that you would need to pass an instance of [CachedCI.Setup]((https://github.com/romanowski/hoarder/blob/master/0.13/hoarder/src/main/scala/org/romanowski/hoarder/actions/CachedCI.scala)) trait that describes you CI flow. For more details please see [scala docs](https://github.com/romanowski/hoarder/blob/master/0.13/hoarder/src/main/scala/org/romanowski/hoarder/actions/CachedCI.scala)
