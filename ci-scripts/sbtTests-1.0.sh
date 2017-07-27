set -o xtrace && \
    sbt ";^^ 1.0.0-RC2; test" && \
    sbt ";^^ 1.0.0-RC2; scripted"