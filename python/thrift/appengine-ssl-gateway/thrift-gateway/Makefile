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

# Deploying the Thrift gateway

REGION = us-central1

KEY_FILE = stunnel.pem
ssl-key: $(KEY_FILE)

$(KEY_FILE):
	$(VERB) openssl req -new -days 365000 -x509 -nodes -out $(KEY_FILE) -keyout $(KEY_FILE) -subj "/C=US/ST=NY/L=New_York/O=Dis/CN=www.example.com" -pubkey
	$(VERB) cp -f $(KEY_FILE) ../appengine

copy-ssl-key-to-gcs: $(KEY_FILE) env_project_id env_bucket
	$(VERB) gsutil mb -p $$PROJECT_ID gs://$$BUCKET || gsutil ls gs://$$BUCKET
	$(VERB) gsutil cp $(KEY_FILE) gs://$$BUCKET/$(KEY_FILE)

DEPLOYMENT_CONFIG = deployment.yaml
DEPLOYMENT = thrift-gateway

update-deployment-config: env_cluster_id env_zone env_bucket env_docker_project_id
	$(VERB) sed -i 's/<CLUSTER_ID>/'$$CLUSTER_ID'/g' $(DEPLOYMENT_CONFIG)
	$(VERB) sed -i 's@<BUCKET>@'"$$BUCKET"'@g' $(DEPLOYMENT_CONFIG)
	$(VERB) sed -i 's@<DOCKER_PROJECT_ID>@'"$$DOCKER_PROJECT_ID"'@g' $(DEPLOYMENT_CONFIG)
	$(VERB) sed -i 's/<ZONE>/'$$ZONE'/g' $(DEPLOYMENT_CONFIG)

deploy-gateway: env_project_id
	$(VERB) gcloud --project $$PROJECT_ID deployment-manager deployments create $(DEPLOYMENT) --config $(DEPLOYMENT_CONFIG)

delete-gateway: env_project_id
	$(VERB) gcloud --project $$PROJECT_ID deployment-manager deployments delete $(DEPLOYMENT)

print-thrift-gateway-lb-ip:
	$(VERB) gcloud --project $$PROJECT_ID compute forwarding-rules describe thrift-gateway-lb --region $(REGION) | grep IPAddress | sed 's/IPAddress: //'

# Docker container for the Thrift gateway

HBASE_VERSION = 1.1.2
HBASE_TAR_GZ = hbase-$(HBASE_VERSION)-bin.tar.gz
HBASE_DIR = hbase-$(HBASE_VERSION)

download-hbase: $(HBASE_TAR_GZ)

$(HBASE_TAR_GZ):
	$(VERB) curl "https://archive.apache.org/dist/hbase/$(HBASE_VERSION)/$(HBASE_TAR_GZ)" -f -o "$@"

install-hbase: $(HBASE_TAR_GZ)
	$(VERB) ./install-hbase.sh

hbase-$(HBASE_VERSION): install-hbase

DOCKER_CONTAINER_NAME = thrift-gateway

docker-build: env_docker_project_id hbase-$(HBASE_VERSION)
	$(VERB) docker build -t gcr.io/$$DOCKER_PROJECT_ID/$(DOCKER_CONTAINER_NAME) .

docker-run: env_project_id env_zone env_cluster_id env_bucket env_docker_project_id
	$(VERB) docker run -ti -e KEY_OBJECT=gs://$$BUCKET/$(KEY_FILE) \
          -e CLUSTER_ID=$$CLUSTER_ID -e PROJECT=$$PROJECT_ID \
          -e ZONE=$$ZONE -p 1090:1090 -p 9090:9090 \
          gcr.io/$$DOCKER_PROJECT_ID/$(DOCKER_CONTAINER_NAME)

docker-pull: env_docker_project_id
	$(VERB) gcloud docker pull gcr.io/$$DOCKER_PROJECT_ID/$(DOCKER_CONTAINER_NAME)

docker-push: env_docker_project_id
	$(VERB) gcloud docker push gcr.io/$$DOCKER_PROJECT_ID/$(DOCKER_CONTAINER_NAME)

clean:
	$(VERB) rm -f $(KEY_FILE) $(HBASE_TAR_GZ) $(HBASE_DIR)
