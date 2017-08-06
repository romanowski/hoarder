set -o xtrace
rm -r -f test-ws
export HOARDER_CI_VERSION="1.0.1-a-test-version"
#Test it!

sbt ^publishLocal && \
    ci-scripts/install-and-clone.sh shapeless && \
    cd test-ws  && \
    sbt preBuild && \
    sbt test:compile && \
    sbt postBuild && \
    sbt preventCompilationStatus
