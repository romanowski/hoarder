set -o xtrace && \
    export HOARDER_BUCKET_PREFIX=$TRAVIS_PULL_REQUEST-1.0 && \
    sbt test && \
    sbt scripted