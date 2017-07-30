#TODO - I need to cross compile this!
set -o xtrace && \
    ci-scripts/publishLocal.sh && \
    echo simulate stash workflow, first clone && \
    git clone -b hoarder-ci https://github.com/romanowski/ensime-server.git test-ws && \
    echo download caches && \
  #  mv .hoarder-cache test-ws && \
    cd test-ws  && \
    git config --global user.email "you@example.com" && \
    git config --global user.name "Your Name" && \
    git merge origin/hoarder-ci-test -m "Test" && \
    echo 'addSbtPlugin("com.github.romanowski" %% "hoarder" % "1.0-for-test")' > project/hoarder.sbt && \
    sbt preBuild && \
    sbt test:compile && \
    sbt postBuild && \
    sbt preventCompilationStatus && \
    echo upload caches && \
    cd .. && \
    mv test-ws/.hoarder-cache . && \
    ls -alR .hoarder-cache