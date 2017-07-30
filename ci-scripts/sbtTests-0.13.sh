set -o xtrace && \
    sbt ";^^ 0.13.16; test" && \
    sbt ";^^ 0.13.16; scripted"