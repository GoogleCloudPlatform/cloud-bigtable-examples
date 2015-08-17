FROM java:7-jdk

ENV GOOGLE_APPLICATION_CREDENTIALS=/hbase/conf/key.json

WORKDIR /

RUN mkdir hbase 
RUN curl http://apache.mesi.com.ar/hbase/1.1.1/hbase-1.1.1-bin.tar.gz \
        | tar -zxC hbase --strip-components=1

RUN mkdir -p hbase/lib/bigtable
RUN curl -0 http://central.maven.org/maven2/com/google/cloud/bigtable/bigtable-hbase-1.1/0.2.0/bigtable-hbase-1.1-0.2.0.jar  \
        -o /hbase/lib/bigtable/bigtable-hbase-1.1-0.2.0.jar
RUN curl -0 http://central.maven.org/maven2/org/mortbay/jetty/alpn/alpn-boot/7.1.3.v20150130/alpn-boot-7.1.3.v20150130.jar \
        -o /hbase/lib/bigtable/alpn-boot-7.1.3.v20150130.jar

RUN echo "export HBASE_CLASSPATH=/hbase/lib/bigtable/bigtable-hbase-1.1-0.2.0.jar" >>/hbase/conf/hbase-env.sh && \
    echo "export HBASE_OPTS=\"${HBASE_OPTS} -Xms1024m -Xmx2048m -Xbootclasspath/p:/hbase/lib/bigtable/alpn-boot-7.1.3.v20150130.jar\""  >>/hbase/conf/hbase-env.sh

ADD . /hbase/conf

ENV JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
ENV PATH /hbase/bin:$PATH
