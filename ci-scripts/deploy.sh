#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
     openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in private-files.zip.enc -out private-files.zip -d
     unzip private-files.zip
     sbt ^publishSigned finalizeRelease
fi