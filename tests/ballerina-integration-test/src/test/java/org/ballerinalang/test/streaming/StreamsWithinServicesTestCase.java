/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.test.streaming;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.ballerinalang.test.BaseTest;
import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.context.Constant;
import org.ballerinalang.test.context.Utils;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.ballerinalang.test.util.TestConstant;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tests the streaming functionality in a service.
 */
public class StreamsWithinServicesTestCase extends BaseTest {
    private static BServerInstance serverInstance;
    private final int servicePort = Constant.DEFAULT_HTTP_PORT;

    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception {
        int[] requiredPorts = new int[]{9090};
        Utils.checkPortsAvailability(requiredPorts);

        String relativePath = new File("src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "streaming" + File.separator +
                "streams-within-services.bal").getAbsolutePath();
        serverInstance = new BServerInstance(balServer);
        serverInstance.startServer(relativePath, new int[]{servicePort});
    }

    @AfterClass(alwaysRun = true)
    private void cleanup() throws Exception {
        serverInstance.removeAllLeechers();
        serverInstance.shutdownServer();
    }

    @Test(description = "Test the service with sample streaming rules")
    public void testStreamsWithinServices() throws Exception {
        int count = 0;
        String responseMsg;
        String requestMessage = "{'message' : 'Hello There'}";

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_JSON);

        for (int i = 0; i < 10; i++) {
            HttpResponse response = HttpClientRequest.doPost(serverInstance
                            .getServiceURLHttp(servicePort, "requests"), requestMessage, headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
            Assert.assertEquals(response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE.toString())
                    , TestConstant.CONTENT_TYPE_JSON, "Content-Type mismatched");
            Assert.assertEquals(response.getData(), "\"{'message' : 'request successfully received'}\"",
                    "Message content mismatched");
            Thread.sleep(100);
        }

        do {
            count++;
            HttpResponse response = HttpClientRequest.doGet(serverInstance
                    .getServiceURLHttp(servicePort, "hosts"), headers);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
            Assert.assertEquals(response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE.toString())
                    , TestConstant.CONTENT_TYPE_JSON, "Content-Type mismatched");
            responseMsg = response.getData();
            Thread.sleep(1000);
        } while (count != 10 || responseMsg.equals("\"{'message' : 'NotAssigned'}\""));
        Assert.assertNotEquals(responseMsg, "\"{'message' : 'NotAssigned'}\"");
    }
}
