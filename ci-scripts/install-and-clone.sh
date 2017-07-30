echo simulate stash workflow, first clone && \
  git clone -b hoarder-ci https://github.com/romanowski/$1.git test-ws && \
  echo 'addSbtPlugin("com.github.romanowski" %% "hoarder" % "'$HOARDER_CI_VERSION'")' > test-ws/project/hoarder.sbt && \
  echo 'addSbtPlugin("com.github.romanowski" %% "hoarder-tests" % "'$HOARDER_CI_VERSION'")' > test-ws/project/hoarder-test.sbt
