/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.test.securelistener;

import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test cases for verifying non specific auth handler with secured listener.
 */
@Test(groups = "secure-listener-test")
public class SecureListenerNonSpecificHandlerTest extends SecureListenerBaseTest {
    private int servicePort = 9091;

    @Test(description = "Authn and authz success test case")
    public void testAuthSuccess() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic aXN1cnU6eHh4");
        HttpResponse response = HttpClientRequest.doGet(serverInstance.getServiceURLHttp(servicePort, "echo/test"),
                headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Authn success and authz failure test case")
    public void testAuthzFailure() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic aXNoYXJhOmFiYw==");
        HttpResponse response = HttpClientRequest.doGet(serverInstance.getServiceURLHttp(servicePort, "echo/test"),
                headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
    }

    @Test(description = "Authn and authz failure test case")
    public void testAuthFailure() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic dGVzdDp0ZXN0MTIz");
        HttpResponse response = HttpClientRequest.doGet(serverInstance.getServiceURLHttp(servicePort, "echo/test"),
                headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
    }
}
