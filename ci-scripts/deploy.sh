#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
      openssl aes-256-cbc -K $encrypted_7e059051908a_key -iv $encrypted_7e059051908a_iv -in ci-scripts/pubring.asc.enc -out ci-scripts/pubring.asc -d
      openssl aes-256-cbc -K $encrypted_7e059051908a_key -iv $encrypted_7e059051908a_iv -in ci-scripts/secring.asc.enc -out ci-scripts/secring.asc -d
      openssl aes-256-cbc -K $encrypted_6c92e0c7ed61_key -iv $encrypted_6c92e0c7ed61_iv -in deploy.sbt.enc -out ../deploy.sbt -d
      sbt ^releaseEarly
fi