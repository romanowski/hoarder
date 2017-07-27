set -o xtrace && \
    sbt ";^^ 0.13.15; test" && \
    sbt ";^^ 0.13.15; scripted"