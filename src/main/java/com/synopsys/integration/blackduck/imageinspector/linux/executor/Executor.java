/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.imageinspector.linux.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.exception.IntegrationException;

@Component
public class Executor {
    private static final Logger logger = LoggerFactory.getLogger(Executor.class);

    public static String[] executeCommand(final String commandString, final Long timeoutMillisec) throws IntegrationException, UnsupportedEncodingException {
        logger.debug(String.format("Executing: %s with timeout %s", commandString, timeoutMillisec));
        final CommandLine cmdLine = CommandLine.parse(commandString);
        return executeCommandLine(commandString, timeoutMillisec, cmdLine);
    }

    public static String[] executeCommand(final List<String> commandParts, final Long timeoutMillisec) throws IntegrationException, UnsupportedEncodingException {
        logger.debug(String.format("Executing: %s with timeout %s", commandParts.get(0), timeoutMillisec));
        final CommandLine cmdLine = new CommandLine(commandParts.get(0));
        for (int i = 1; i < commandParts.size(); i++) {
            logger.debug(String.format("Adding arg to %s command line: %s", commandParts.get(0), commandParts.get(i)));
            cmdLine.addArgument(commandParts.get(i), false);
        }
        return executeCommandLine(commandParts.get(0), timeoutMillisec, cmdLine);
    }

    private static String[] executeCommandLine(final String commandString, final Long timeoutMillisec, final CommandLine cmdLine) throws IntegrationException, UnsupportedEncodingException {
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(1);
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMillisec);
        executor.setWatchdog(watchdog);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);
        int exitValue = -1;
        try {
            exitValue = executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            exitValue = e.getExitValue();
            logger.trace(String.format("Execution of command: %s: ExecutionException: %s; exitCode: %d; Continuing anyway...", commandString, e.getMessage(), exitValue));
            // throw new IntegrationException(String.format("Execution of command: %s: ExecutionException: %s", commandString, e.getMessage()));
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Execution of command: %s: IOException: %s", commandString, e.getMessage()));
        }
        if (watchdog.killedProcess()) {
            throw new IntegrationException(String.format("Execution of command: %s with timeout %d timed out", commandString, timeoutMillisec, exitValue));
        }
        if (exitValue == 0) {
            logger.debug(String.format("Success executing command: %s", commandString));
        } else {
            throw new IntegrationException(String.format("Execution of command: %s: Error code: %d: stderr: %s", commandString, exitValue, errorStream.toString(StandardCharsets.UTF_8.name())));
        }

        logger.debug(String.format("Command output: %s", outputStream.toString(StandardCharsets.UTF_8.name())));
        return outputStream.toString(StandardCharsets.UTF_8.name()).split(System.lineSeparator());
    }

}
