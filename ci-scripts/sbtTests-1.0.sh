set -o xtrace && \
    export HOARDER_BUCKET_PREFIX=$TRAVIS_BRANCH
    sbt test && \
    sbt scripted