ci-scripts/publishLocal.sh && \ && \
  echo simulate stash workflow, first clone && \
  git clone -b hoarder-ci https://github.com/romanowski/ensime-server.git test-ws && \
  echo 'addSbtPlugin("com.github.romanowski" %% "hoarder" % "1.0-for-test")' > test-ws/project/hoarder.sbt && \
  echo 'addSbtPlugin("com.github.romanowski" %% "hoarder-tests" % "1.0-for-test")' > test-ws/project/hoarder-test.sbt
