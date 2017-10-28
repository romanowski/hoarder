set -o xtrace
rm -r -f test-ws
export HOARDER_CI_VERSION="1.0.1-a-test-version"

sbt ^publishLocal && \
    ci-scripts/install-and-clone.sh ensime-server && \
    cd test-ws  && \
    echo 'addSbtPlugin("com.github.romanowski" %% "hoarder-amazon" % "'$HOARDER_CI_VERSION'")' > project/hoarder-amazon.sbt && \
    sbt preventCompilationStatus && \
    sbt preBuild && \
    sbt test:compile && \
    sbt postBuild
