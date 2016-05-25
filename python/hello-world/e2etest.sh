#!/usr/bin/env bash

set -e
set -x

if [ -z "$GCLOUD_PROJECT" ]; then
  echo "GCLOUD_PROJECT must be set."
  exit 1
fi

if [ -z "$BIGTABLE_CLUSTER" ]; then
  echo "BIGTABLE_CLUSTER must be set."
  exit 1
fi

if [ -z "$BIGTABLE_ZONE" ]; then
  echo "BIGTABLE_ZONE must be set."
  exit 1
fi

OUTPUT=$(mktemp)
VENV_DIR="${OUTPUT}_venv"
virtualenv $VENV_DIR
function finish {
  rm "$OUTPUT"
  rm -rf "$VENV_DIR"
}
trap finish EXIT

source "${VENV_DIR}/bin/activate"
pip install -r requirements.txt

python hello.py \
    -p "$GCLOUD_PROJECT" \
    -c "$BIGTABLE_CLUSTER" \
    -z "$BIGTABLE_ZONE" \
    | tee "$OUTPUT"
grep 'Create table Hello-Bigtable' "$OUTPUT"
grep 'Delete table Hello-Bigtable' "$OUTPUT"
grep 'greeting0: Hello World!' "$OUTPUT"
grep 'greeting1: Hello Cloud Bigtable!' "$OUTPUT"
grep 'greeting2: Hello HappyBase!' "$OUTPUT"

