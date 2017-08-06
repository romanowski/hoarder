#TODO - I need to cross compile this!
set -o xtrace
rm -r -f test-ws
export HOARDER_CI_VERSION="1.0.1-a-test-version"

sbt '^publishLocal' && \
    ci-scripts/install-and-clone.sh ensime-server && \
    echo download caches && \
    mv .hoarder-cache test-ws && \
    cd test-ws  && \
    git config --global user.email "you@example.com" && \
    git config --global user.name "Your Name" && \
    git merge origin/hoarder-ci-test -m "Test" && \
    sbt preBuild && \
    sbt test:compile && \
    sbt postBuild && \
    sbt preventCompilationStatus && \
    echo upload caches && \
    cd .. && \
    mv test-ws/.hoarder-cache .