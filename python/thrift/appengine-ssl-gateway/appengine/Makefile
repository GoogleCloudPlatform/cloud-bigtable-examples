#!/bin/bash -eu
#
# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################

include ../common.mk

THIRD_PARTY_PYTHON_MANAGED_LIBS = third_party/python/managed

pip-install:
	$(VERB) mkdir -p $(THIRD_PARTY_PYTHON_MANAGED_LIBS)
	$(VERB) pip install -r requirements.txt -t $(THIRD_PARTY_PYTHON_MANAGED_LIBS)

KEY_FILE = stunnel.pem

# $(KEY_FILE) should have been produced by an earlier step and copied to this
# directory as well as to a GCS bucket. If it's missing, the user skipped that
# step.
deploy: env_project_id
	$(VERB) if ! [ -e $(KEY_FILE) ]; then echo >&2 "$(KEY_FILE) missing"; exit 1; fi
	$(VERB) gcloud app deploy \
	            --project $$PROJECT_ID --version thrift-gateway --no-promote

clean:
	$(VERB) rm -rf $(THIRD_PARTY_PYTHON_MANAGED_LIBS)
