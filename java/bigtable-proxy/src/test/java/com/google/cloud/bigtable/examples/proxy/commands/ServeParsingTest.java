package com.google.cloud.bigtable.examples.proxy.commands;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import picocli.CommandLine;

@RunWith(JUnit4.class)
public class ServeParsingTest {
  @Test
  public void testMinimalArgs() {
    Serve serve = new Serve();
    new CommandLine(serve).parseArgs("--listen-port=1234", "--metrics-project-id=fake-project");

    assertThat(serve.listenPort).isEqualTo(1234);
    assertThat(serve.metricsProjectId).isEqualTo("fake-project");
    assertThat(serve.userAgent).isEqualTo("bigtable-java-proxy");
    assertThat(serve.dataEndpoint).isEqualTo(Endpoint.create("bigtable.googleapis.com", 443));
    assertThat(serve.adminEndpoint).isEqualTo(Endpoint.create("bigtableadmin.googleapis.com", 443));
  }

  @Test
  public void testDataEndpointOverride() {
    Serve serve = new Serve();
    new CommandLine(serve)
        .parseArgs(
            "--listen-port=1234",
            "--metrics-project-id=fake-project",
            "--bigtable-data-endpoint=example.com:1234");

    assertThat(serve.listenPort).isEqualTo(1234);
    assertThat(serve.dataEndpoint).isEqualTo(Endpoint.create("example.com", 1234));
  }

  @Test
  public void testAdminDataEndpointOverride() {
    Serve serve = new Serve();
    new CommandLine(serve)
        .parseArgs(
            "--listen-port=1234",
            "--metrics-project-id=fake-project",
            "--bigtable-admin-endpoint=example.com:1234");

    assertThat(serve.listenPort).isEqualTo(1234);
    assertThat(serve.adminEndpoint).isEqualTo(Endpoint.create("example.com", 1234));
  }

  @Test
  public void testMetricsProjectIdOverride() {
    Serve serve = new Serve();
    new CommandLine(serve)
        .parseArgs("--listen-port=1234", "--metrics-project-id=other-fake-project");
    assertThat(serve.metricsProjectId).isEqualTo("other-fake-project");
  }
}
