#!/usr/bin/env sh
# Licensed to the Apache Software Foundation (ASF) under the Apache License, Version 2.0.
# Bootstrap a pinned Maven distribution when Maven is not installed locally.
set -eu

BASE_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -r "$PROPERTIES" ]; then
  printf '%s\n' "Missing Maven wrapper properties: $PROPERTIES" >&2
  exit 1
fi

DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES" | tr -d '\r' | head -n 1)
if [ -z "$DISTRIBUTION_URL" ]; then
  printf '%s\n' "distributionUrl is not configured" >&2
  exit 1
fi

ARCHIVE_NAME=${DISTRIBUTION_URL##*/}
MAVEN_NAME=${ARCHIVE_NAME%-bin.zip}
MAVEN_HOME="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/$MAVEN_NAME"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
  TEMP_DIR=$(mktemp -d)
  cleanup() { rm -rf "$TEMP_DIR"; }
  trap cleanup EXIT HUP INT TERM

  mkdir -p "$(dirname -- "$MAVEN_HOME")"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$DISTRIBUTION_URL" --output "$TEMP_DIR/$ARCHIVE_NAME"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$DISTRIBUTION_URL" --output-document "$TEMP_DIR/$ARCHIVE_NAME"
  else
    printf '%s\n' "Maven wrapper requires curl or wget" >&2
    exit 1
  fi

  unzip -q "$TEMP_DIR/$ARCHIVE_NAME" -d "$TEMP_DIR"
  if [ ! -x "$TEMP_DIR/$MAVEN_NAME/bin/mvn" ]; then
    printf '%s\n' "Downloaded Maven distribution is incomplete" >&2
    exit 1
  fi

  if [ ! -d "$MAVEN_HOME" ]; then
    mv "$TEMP_DIR/$MAVEN_NAME" "$MAVEN_HOME"
  fi
fi

exec "$MAVEN_BIN" "$@"
