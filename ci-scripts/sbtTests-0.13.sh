set -o xtrace && \
    cd 0.13 && \
    sbt ";^^ 0.13.15; test" && \
    sbt ";^^ 0.13.15; scripted"