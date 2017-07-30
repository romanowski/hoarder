set -o xtrace && \
    sbt ";set version.in(hoarder) :=\"1.0-for-test\";^hoarderCore/publishLocal;^hoarder/publishLocal"