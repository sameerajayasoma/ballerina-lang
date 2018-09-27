'use strict';
/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

// The module 'assert' provides assertion methods from node
import * as assert from 'assert';

// You can import and use all API from the 'vscode' module
// as well as import your extension to test it
import ballerinaExtention from '../../core/ballerina-extension';
import { getBallerinaVersion, getBallerinaHome} from '../test-util';

// Ballerina tools distribution will be copied to following location by maven
const testBallerinaHome = getBallerinaHome();
const testBallerinaVersion = getBallerinaVersion();

// Defines a Mocha test suite to group tests of similar kind together
suite("Ballerina Extension Core Tests", function () {


    test("Test isValidBallerinaHome", function () {
        assert.equal(ballerinaExtention.isValidBallerinaHome(testBallerinaHome), true);
        assert.equal(ballerinaExtention.isValidBallerinaHome(testBallerinaHome + '../'), false);
    });

    test("Test autoDitectBallerinaHome", function () {
        // Following should not throw an error all times.
        const path = ballerinaExtention.autoDitectBallerinaHome();
        if (path) {
            assert.equal(ballerinaExtention.isValidBallerinaHome(path), true);
        }
    });

    test("Test getBallerinaVersion", function () {
        const ditected = ballerinaExtention.getBallerinaVersion(testBallerinaHome)
        assert.equal(ditected, testBallerinaVersion);
    });

});
