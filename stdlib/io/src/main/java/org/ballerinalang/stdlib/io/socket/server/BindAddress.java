/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.io.socket.server;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.stdlib.io.socket.SelectorManager;
import org.ballerinalang.stdlib.io.socket.SocketConstants;
import org.ballerinalang.stdlib.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;

/**
 * Extern function for ServerSocket bind address.
 *
 * @since 0.971.1
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "io", functionName = "bindAddress",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "ServerSocket",
                             structPackage = SocketConstants.SOCKET_PACKAGE),
        args = {@Argument(name = "port", type = TypeKind.INT),
                @Argument(name = "interface", type = TypeKind.STRING)
        },
        isPublic = true
)
public class BindAddress extends BlockingNativeCallableUnit {

    private static final Logger log = LoggerFactory.getLogger(BindAddress.class);

    @Override
    public void execute(Context context) {
        BMap<String, BValue> serverSocketStruct;
        int port = (int) context.getIntArgument(0);
        try {
            serverSocketStruct = (BMap<String, BValue>) context.getRefArgument(0);
            BValue networkInterface = context.getNullableRefArgument(1);
            ServerSocketChannel serverSocket = (ServerSocketChannel) serverSocketStruct
                    .getNativeData(SocketConstants.SERVER_SOCKET_KEY);
            if (networkInterface == null) {
                serverSocket.bind(new InetSocketAddress(port), 1);
            } else {
                serverSocket.bind(new InetSocketAddress(networkInterface.stringValue(), port), 1);
            }
            final Selector selector = SelectorManager.getInstance();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT, serverSocket);
            SelectorManager.start();
        } catch (ConnectionPendingException e) {
            String message = "Socket initialization already in process. Unable to bind to the port.";
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (AlreadyBoundException e) {
            String message = "Unable to bind to the port: " + port + ". Socket is already bound.";
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (UnsupportedAddressTypeException e) {
            String message = "This socket address doesn't support for a TCP connection.";
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (ClosedChannelException e) {
            String message = "Socket connection is already closed.";
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (IOException e) {
            String message = "Error occurred while bind to the socket address: " + e.getMessage();
            log.error(message, e);
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (SecurityException e) {
            String message = "Unknown error occurred.";
            log.error(message, e);
            context.setReturnValues(IOUtils.createError(context, message));
        } catch (Throwable e) {
            String message = "An error occurred.";
            log.error(message, e);
            context.setReturnValues(IOUtils.createError(context, message));
        }
    }
}
