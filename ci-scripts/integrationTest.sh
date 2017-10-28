#TODO - I need to cross compile this!
set -o xtrace
rm -r -f test-ws
export HOARDER_CI_VERSION="1.0.1-a-test-version"

sbt '^publishLocal' && \
    ls -alR test-ws
    ci-scripts/install-and-clone.sh ensime-server && \
    cp ci-scripts/HoarderS3Test.scala test-ws\project
    cd test-ws  && \
    echo object IntegrationTest extends org.romanowski.hoarder.tests.IntegrationTestFlowBase > project/IntegrationTest.scala
    sbt postBuild && \
    export HOARDER_CACHE_EXPORTED=true && \
    sbt clean
    sbt preventCompilationStatus && \
    sbt preBuild && \
    sbt test:compile