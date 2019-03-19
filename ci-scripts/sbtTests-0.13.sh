set -o xtrace && \
    export HOARDER_BUCKET_PREFIX=$TRAVIS_PULL_REQUEST-0.13 && \
    sbt ";^^ 0.13.16; test" && \
    sbt ";^^0.13.16;hoarderTests/scripted"