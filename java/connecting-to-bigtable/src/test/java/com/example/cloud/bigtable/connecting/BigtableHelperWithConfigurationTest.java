package com.example.cloud.bigtable.connecting;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class BigtableHelperWithConfigurationTest {
  private final String projectId = System.getProperty("bigtable.projectID");
  private final String instanceId = System.getProperty("bigtable.instanceID");

  @Test
  public void connection() throws Exception {
    BigtableHelperWithConfiguration helper = new BigtableHelperWithConfiguration();
    helper.connect();

    assertThat(helper.connection.toString()).contains("project="+projectId);
    assertThat(helper.connection.toString()).contains("instance="+instanceId);
  }
}
