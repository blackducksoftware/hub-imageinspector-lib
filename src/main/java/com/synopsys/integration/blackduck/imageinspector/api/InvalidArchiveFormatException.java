/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.imageinspector.api;

import com.synopsys.integration.exception.IntegrationException;

public class InvalidArchiveFormatException extends IntegrationException {
    private static final long serialVersionUID = 1L;

    public InvalidArchiveFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidArchiveFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArchiveFormatException(String message) {
        super(message);
    }

    public InvalidArchiveFormatException(Throwable cause) {
        super(cause);
    }

}
