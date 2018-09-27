/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.packerina.cmd;

import org.ballerinalang.launcher.BLauncherCmd;
import org.ballerinalang.packerina.SearchUtils;
import picocli.CommandLine;

import java.io.PrintStream;
import java.util.List;

import static org.ballerinalang.launcher.LauncherUtils.createUsageExceptionWithHelp;
import static org.ballerinalang.packerina.cmd.Constants.SEARCH_COMMAND;
import static org.ballerinalang.runtime.Constants.SYSTEM_PROP_BAL_DEBUG;

/**
 * This class represents the "ballerina search" command.
 *
 * @since 0.964
 */
@CommandLine.Command(name = SEARCH_COMMAND, description = "search for packages within Ballerina Central")
public class SearchCommand implements BLauncherCmd {
    private static PrintStream outStream = System.err;

    @CommandLine.Parameters
    private List<String> argList;

    @CommandLine.Option(names = {"--help", "-h"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--debug", hidden = true)
    private String debugPort;

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(SEARCH_COMMAND);
            outStream.println(commandUsageInfo);
            return;
        }

        if (null != debugPort) {
            System.setProperty(SYSTEM_PROP_BAL_DEBUG, debugPort);
        }

        if (argList == null || argList.size() == 0) {
            throw createUsageExceptionWithHelp("no keyword given");
        }

        if (argList.size() > 1) {
            throw createUsageExceptionWithHelp("too many arguments");
        }

        String searchArgs = argList.get(0);
        SearchUtils.searchInCentral(searchArgs);
        Runtime.getRuntime().exit(0);
    }

    @Override
    public String getName() {
        return SEARCH_COMMAND;
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("searches for packages within Ballerina Central \n");
    }

    @Override
    public void printUsage(StringBuilder out) {
        out.append(" ballerina search [<org>|<package>|<text>] \n");
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    @Override
    public void setSelfCmdParser(CommandLine selfCmdParser) {
    }
}
