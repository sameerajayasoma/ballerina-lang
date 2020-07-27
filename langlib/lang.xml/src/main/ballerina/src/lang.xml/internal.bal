// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/java;

# Represent the iterator type returned when `iterator` method is invoked.
type XMLIterator object {

    private xml m;

    public function init(xml m) {
        self.m = m;
    }

    # Return next member or nil if end of iteration is reached.
    # + return - iterator result
    public function next() returns record {| (xml|string) value; |}? {
        return externNext(self);
    }
};

function externNext(XMLIterator iterator) returns record {| (xml|string) value; |}? = @java:Method {
    class: "org.ballerinalang.langlib.xml.Next",
    name: "next"
} external;
