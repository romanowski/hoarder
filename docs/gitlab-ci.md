# Stash and gitlab-ci

It is possible to use the stash workflow to avoid recompiling sources multiple
times in a single gitlab-ci pipeline.  For instance if you have different jobs
in the your pipeline which run different variations of `sbt test`.

`sbt-hoarder` works largely out of the box.
For convenience if you have a multi-project build, you may wish to change
the default stash directory to be at the top level of your project like this:
```
globalStashLocation := (baseDirectory in ThisBuild).value / "sbt-stash"
```

Then you should use `artifacts:` and `dependencies:` as in the following
example `gitlab-ci.yaml`:

```
stages:
  - quick
  - slow

unit-test:
  stage: quick
  artifacts:
    paths:
      - sbt-stash
    expire_in: 1 day
  script:
    - sbt clean test:compile stash Common/test

slow-integration-test-one:
  stage: slow
  dependencies:
    - unit-test
  script:
    - sbt stashApply 'IntegrationTestOne/test'

slow-integration-test-two:
  stage: slow
  dependencies:
    - unit-test
  script:
    - sbt stashApply 'IntegrationTestTwo/test'
```

## Use `artifacts:` not `cache:`

Gitlab `cache` at best provided on a "best effort" basis, and in any case is only
cached per-runner, so in the presence of multiple runners it will be unlikely
to be available for later jobs.  When using `cache` also beware that the runner
logs say cache was found even when no files were actually provided (for instance
because the cache is on a different runner)

Even in a single runner setup, "best effort" combines badly with `sbt-hoarder`'s stash
workflow: `sbt stashApply` will fail if a stash is not found rather than silently
succeeding without using a stash.  This would generally cause your pipeline to fail.

None of this is an issue if you use `artifacts:` as in the example above. 

(A valid related use of `cache` is to avoid re-downloading
external dependencies.  You could cache your `~/.ivy` directory.)

## Alternatives

You may not need sbt-hoarder at all:

- Simplest is to combine all your sbt tasks into one gitlab-ci
  job.  Then no caching is required.
  
- You can also avoid recompilation by having your compilation job
  publish its jar to a repository such as Nexus.
