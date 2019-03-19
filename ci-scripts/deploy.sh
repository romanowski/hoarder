#!/usr/bin/env bash

if ([ "$TRAVIS_BRANCH" = 'master' ] || [ "$TRAVIS_TAG" != "" ]) && [ "$TRAVIS_PULL_REQUEST" = 'false' ] && [ "$scriptName" = 'sbtTests-1.0' ]; then
     echo Deploying...
     openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in cred.zip.enc -out private-files.zip -d
     unzip private-files.zip
     sbt ^publishSigned finalizeRelease
else
     echo Deployment skipped: TB="$TRAVIS_BRANCH" TT="$TRAVIS_TAG" PR="$TRAVIS_PULL_REQUEST" SN="$scriptName"
fi