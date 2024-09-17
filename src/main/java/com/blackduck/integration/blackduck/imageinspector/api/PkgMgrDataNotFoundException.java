/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api;

import com.blackduck.integration.exception.IntegrationException;

public class PkgMgrDataNotFoundException extends IntegrationException {
    private static final long serialVersionUID = 1L;

    public PkgMgrDataNotFoundException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PkgMgrDataNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PkgMgrDataNotFoundException(final String message) {
        super(message);
    }

    public PkgMgrDataNotFoundException(final Throwable cause) {
        super(cause);
    }
}
