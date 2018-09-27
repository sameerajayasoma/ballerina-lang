// Copyright (c) 2018 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// //www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

import ballerina/http;
import ballerina/log;
import ballerina/runtime;

public int forceCloseStateCount = 0;

endpoint http:Listener circuitBreakerEP02 {
    port:9308
};

endpoint http:Client unhealthyClientEP {
    url: "http://localhost:8088",
    circuitBreaker: {
        rollingWindow: {
            timeWindowMillis: 60000,
            bucketSizeMillis: 20000,
            requestVolumeThreshold: 2
        },
        failureThreshold: 0.6,
        resetTimeMillis: 1000,
        statusCodes: [501, 502, 503]
    },

    timeoutMillis: 2000
};

@http:ServiceConfig {
    basePath: "/cb"
}
service<http:Service> circuitbreaker02 bind circuitBreakerEP02 {

    @http:ResourceConfig {
        methods: ["GET", "POST"],
        path: "/forceclose"
    }
    invokeForceClose(endpoint caller, http:Request request) {
        http:CircuitBreakerClient cbClient = check <http:CircuitBreakerClient>unhealthyClientEP.getCallerActions();
        forceCloseStateCount++;
        runtime:sleep(1000);
        if (forceCloseStateCount == 3) {
            runtime:sleep(5000);
            cbClient.forceClose();
        }
        var backendRes = unhealthyClientEP->forward("/unhealthy", request);
        match backendRes {
            http:Response res => {
                caller->respond(res) but {
                    error e => log:printError("Error sending response", err = e)
                };
            }
            error responseError => {
                http:Response response = new;
                response.statusCode = http:INTERNAL_SERVER_ERROR_500;
                response.setPayload(responseError.message);
                caller->respond(response) but {
                    error e => log:printError("Error sending response", err = e)
                };
            }
        }
    }
}

@http:ServiceConfig { basePath: "/unhealthy" }
service<http:Service> unhealthyService bind { port: 8088 } {
    @http:ResourceConfig {
        methods: ["GET", "POST"],
        path: "/"
    }
    sayHello(endpoint caller, http:Request req) {
        http:Response res = new;
        if (forceCloseStateCount <= 3) {
            runtime:sleep(5000);
        } else {
            res.setPayload("Hello World!!!");
        }
        caller->respond(res) but {
            error e => log:printError("Error sending response from mock service", err = e)
        };
    }
}
