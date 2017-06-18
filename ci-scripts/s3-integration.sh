set -o xtrace && \
    ci-scripts/install-and-clone.sh shapeless && \
    cd test-ws  && \
    sbt preBuild && \
    sbt test:compile && \
    sbt postBuild && \
    sbt preventCompilationStatus
