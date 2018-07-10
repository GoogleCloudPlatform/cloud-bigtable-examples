package com.example.cloud.bigtable.connecting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class BigtableHelperWithConfigurationTest {

    @Test
    public void connection() throws Exception {
        BigtableHelperWithConfiguration helper = new BigtableHelperWithConfiguration();
        helper.connect();

        helper.connection.getAdmin().listTables();
    }
}
