#!/usr/bin/env bash
# This script provisions a fresh GCE instance to install a REST service that
# connect to an HBase Thrift server, implemented in Python with the Flask
# web framework

# Make sure our package manager is up to date
sudo apt-get update

# Download our system dependencies
# Most of these are for building Thrift except for pip at the end
sudo apt-get install -y build-essential libboost-dev libboost-test-dev libboost-program-options-dev libboost-system-dev libboost-filesystem-dev libevent-dev automake libtool flex bison pkg-config g++ libssl-dev python-pip

# The Debian instructions on the Thrift website recommend this automake build
# https://thrift.apache.org/docs/install/debian
cd /tmp
wget http://ftp.debian.org/debian/pool/main/a/automake-1.14/automake_1.14.1-4_all.deb
sudo dpkg -i automake_1.14.1-4_all.deb

# Download Thrift, building it from source is the recommended way, and we need
# to copy the language bindings anyway
curl http://apache.mirrors.ionfish.org/thrift/0.9.2/thrift-0.9.2.tar.gz  | tar zx
cd thrift-0.9.2/
 ./configure
make
sudo make install

# We will by default setup the directory in the home directory
cd ~

# Copy the Thrift Language bindings over
mkdir ~/thrift
cp -r /tmp/thrift-0.9.2/lib/py/src/* thrift/

# Generate our HBbase thrift bindings for Python
thrift -gen py third_party/Hbase.thrift

# Now let's set up Flask
# General best practice is to use virtualenv for isolated Python environments
sudo pip install virtualenv

# We throw our virtualenv right in this folder although we could move it out of the way
virtualenv flaskthrift
source flaskthrift/bin/activate

# Install python requirements (Flask, requests)
pip install -r requirements.txt
