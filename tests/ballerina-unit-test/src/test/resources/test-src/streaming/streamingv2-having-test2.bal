// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/runtime;
import ballerina/io;
import ballerina/streams;

type Employee record {
    string name;
    int age;
};

int index = 0;
stream<Employee> inputStream;
stream<Employee> outputStream;
Employee[] globalEmployeeArray = [];

function startFilterQuery() returns (Employee[]) {

    Employee[] employeeArr = [];
    Employee t1 = { name: "Mohan", age: 29 };
    Employee t2 = { name: "Nisala", age: 25 };
    Employee t3 = { name: "Gimantha", age: 28 };
    Employee t4 = { name: "Grainier", age: 26 };
    employeeArr[0] = t1;
    employeeArr[1] = t2;
    employeeArr[2] = t3;
    employeeArr[3] = t4;

    testFilterQuery();

    outputStream.subscribe(printTeachers);
    foreach e in employeeArr {
        inputStream.publish(e);
    }

    runtime:sleep(1000);
    return globalEmployeeArray;
}

function testFilterQuery() {

    forever {
        from inputStream
        select inputStream.name, inputStream.age
        having getAge(age) > getMaxAge() && getAge(age) > 25
        => (Employee[] emp) {
            outputStream.publish(emp);
        }
    }
}

function getMaxAge() returns int  {
    return 25;
}

function getAge(int age) returns int {
    return age;
}

function printTeachers(Employee e) {
    addToGlobalEmployeeArray(e);
}

function addToGlobalEmployeeArray(Employee e) {
    globalEmployeeArray[index] = e;
    index = index + 1;
}
