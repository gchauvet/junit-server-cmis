/*
 * Copyright 2016 gsdenys@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.gsdenys;

import com.github.gsdenys.runner.Configure;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Test for the {@link CmisInMemoryRunner} class
 *
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 0.0.1
 * @since 0.0.1
 */
@RunWith(CmisInMemoryRunner.class)
@Configure(port = 8080)
public class CmisInMemoryRunnerTest {
    @Test
    public void getSession() throws Exception {
        Session session = CmisInMemoryRunner.getSession();

       RepositoryInfo info = session.getRepositoryInfo();
       Assert.assertNotNull("the info should not be null", info);
    }

    @Test
    public void getSession1() throws Exception {
        Session session = CmisInMemoryRunner.getSession("A1");

        RepositoryInfo info = session.getRepositoryInfo();
        Assert.assertNotNull("the info should not be null", info);
    }

    @Test
    public void getCmisURI() throws Exception {
        URI uriBase = CmisInMemoryRunner.getCmisURI();

        Assert.assertTrue("The URL should ends with ':8080/cmis/atom11'", uriBase.toString().endsWith(":8080/cmis/atom11"));
    }

    @Test
    public void testConnection() throws Exception {
        URL url = CmisInMemoryRunner.getCmisURI().toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        Assert.assertEquals("The Response code should be 200", conn.getResponseCode(), 200);

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

        Assert.assertNotNull("The buffered reader should be null", br);

        conn.disconnect();
    }

    @Test
    public void getCmisPort() throws Exception {
        Integer port = CmisInMemoryRunner.getCmisPort();

        Assert.assertNotNull("The object should not be null", port);

        Assert.assertEquals(
                "The port should be 8080",
                java.util.Optional.ofNullable(port),
                java.util.Optional.ofNullable(8080)
        );
    }

    @Test
    public void getPortDefinedByUser() throws Exception {
        Assert.assertEquals(
                "The port should be 8080",
                java.util.Optional.ofNullable(CmisInMemoryRunner.getCmisPort()),
                java.util.Optional.ofNullable(8080)
        );
    }
}