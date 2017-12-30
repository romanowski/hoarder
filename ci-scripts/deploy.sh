#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
     openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in encrypted/private-secring.asc -out private-secring.asc -d
     openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in encrypted/private-pubring.asc -out private-pubring.asc -d
     openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in encrypted/private-sonatype-cred.sbt -out private-sonatype-cred.sbt -d
     sbt ^publishSigned
     sbt sonatypeRelease
fi