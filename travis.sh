#!/usr/bin/env bash
# Copyright 2016 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Note: travis currently does not support listing more than one language so
# this cheats and claims to only be cpp.  If they add multiple language
# support, this should probably get updated to install steps and/or
# rvm/gemfile/jdk/etc. entries rather than manually doing the work.

# Modeled after:
# https://github.com/google/protobuf/blob/master/travis.sh

add_ppa() {
  ppa=$1
  # Install the apt-add-repository command.
  sudo apt-get -qqy install \
      software-properties-common
  sudo apt-add-repository -y "${ppa}"
  sudo apt-get -qq update || true
}

use_java() {
  version=$1
  case "$version" in
    jdk8)
      add_ppa 'ppa:openjdk-r/ppa'
      sudo apt-get -qqy install openjdk-8-jdk
      export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH
      ;;
    oracle8)
      echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | \
          sudo debconf-set-selections
      add_ppa 'ppa:webupd8team/java'
      sudo apt-get -o Dpkg::Options::="--force-overwrite" -qqy install \
          oracle-java8-installer
      export PATH=/usr/lib/jvm/java-8-oracle/bin:$PATH
      ;;
  esac

  which java
  java -version
}

build_java() {
  (
  cd java
  mvn --batch-mode clean verify -DskipTests -Dexec.skip=true | egrep -v "(^\[INFO\] Download|^\[INFO\].*skipping)"
  )
}

build_python() {
  sudo pip install --upgrade pip wheel virtualenv
  sudo pip install --upgrade nox-automation
  (
  cd python
  nox --stop-on-first-error --session lint
  )
}

print_usage() {
  echo "
Usage: $0 { java_jdk8 |
            java_oracle8 |
            python }
"
}

# -------- main --------

if [ "$#" -ne 1 ]; then
  print_usage
  exit 1
fi

set -e  # exit immediately on error
set -x  # display all commands
case $1 in
java_jdk8)
  use_java jdk8
  build_java
  ;;
java_oracle8)
  use_java oracle8
  build_java
  ;;
python)
  build_python
  ;;
*)
  print_usage
  exit 1
  ;;
esac
