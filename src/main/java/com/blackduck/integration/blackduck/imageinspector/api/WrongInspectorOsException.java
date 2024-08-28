/*
 * hub-imageinspector-lib
 *
 * Copyright (c) 2024 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.blackduck.imageinspector.api;

import com.synopsys.integration.exception.IntegrationException;

public class WrongInspectorOsException extends IntegrationException {
    private static final long serialVersionUID = -1109859596321015457L;
    private final ImageInspectorOsEnum correctInspectorOs;

    public WrongInspectorOsException(final ImageInspectorOsEnum correctInspectorOs) {
        super();
        this.correctInspectorOs = correctInspectorOs;
    }

    public WrongInspectorOsException(final ImageInspectorOsEnum correctInspectorOs, final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.correctInspectorOs = correctInspectorOs;
    }

    public WrongInspectorOsException(final ImageInspectorOsEnum correctInspectorOs, final String message, final Throwable cause) {
        super(message, cause);
        this.correctInspectorOs = correctInspectorOs;
    }

    public WrongInspectorOsException(final ImageInspectorOsEnum correctInspectorOs, final String message) {
        super(message);
        this.correctInspectorOs = correctInspectorOs;
    }

    public WrongInspectorOsException(final ImageInspectorOsEnum correctInspectorOs, final Throwable cause) {
        super(cause);
        this.correctInspectorOs = correctInspectorOs;
    }

    public ImageInspectorOsEnum getcorrectInspectorOs() {
        return correctInspectorOs;
    }
}
