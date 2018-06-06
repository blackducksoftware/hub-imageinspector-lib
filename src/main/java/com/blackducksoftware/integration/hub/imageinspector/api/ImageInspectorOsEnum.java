/**
 * hub-imageinspector-lib
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.imageinspector.api;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.imageinspector.lib.OperatingSystemEnum;

public enum ImageInspectorOsEnum {
    UBUNTU(OperatingSystemEnum.UBUNTU),
    CENTOS(OperatingSystemEnum.CENTOS),
    ALPINE(OperatingSystemEnum.ALPINE);

    private final OperatingSystemEnum rawOs;

    private ImageInspectorOsEnum(final OperatingSystemEnum rawOs) {
        this.rawOs = rawOs;
    }

    public static ImageInspectorOsEnum determineOperatingSystem(String operatingSystemName) {
        ImageInspectorOsEnum result = null;
        if (!StringUtils.isBlank(operatingSystemName)) {
            operatingSystemName = operatingSystemName.toUpperCase();
            result = ImageInspectorOsEnum.valueOf(operatingSystemName);
        }
        return result;
    }

    public OperatingSystemEnum getRawOs() {
        return rawOs;
    }
}
