/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.linux;

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
public class CmdExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String[] executeCommand(final List<String> commandParts, final Long timeoutMillisec) throws IntegrationException, UnsupportedEncodingException {
        logger.debug(String.format("Executing: %s with timeout %s", commandParts.get(0), timeoutMillisec));
        final CommandLine cmdLine = new CommandLine(commandParts.get(0));
        for (int i = 1; i < commandParts.size(); i++) {
            logger.debug(String.format("Adding arg to %s command line: %s", commandParts.get(0), commandParts.get(i)));
            cmdLine.addArgument(commandParts.get(i), false);
        }
        return executeCommandLine(commandParts.get(0), timeoutMillisec, cmdLine);
    }

    private String[] executeCommandLine(final String commandString, final Long timeoutMillisec, final CommandLine cmdLine) throws IntegrationException, UnsupportedEncodingException {
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
        } catch (final IOException e) {
            throw new IntegrationException(String.format("Execution of command: %s: IOException: %s", commandString, e.getMessage()));
        }
        if (watchdog.killedProcess()) {
            throw new IntegrationException(String.format("Execution of command: %s with timeout %d timed out", commandString, timeoutMillisec));
        }
        if (exitValue == 0) {
            logger.debug(String.format("Success executing command: %s", commandString));
        } else {
            throw new IntegrationException(String.format("Execution of command: %s: Error code: %d: stderr: %s", commandString, exitValue, errorStream.toString(StandardCharsets.UTF_8.name())));
        }

        logger.trace(String.format("Command output: %s", outputStream.toString(StandardCharsets.UTF_8.name())));
        return outputStream.toString(StandardCharsets.UTF_8.name()).split(System.lineSeparator());
    }

}
