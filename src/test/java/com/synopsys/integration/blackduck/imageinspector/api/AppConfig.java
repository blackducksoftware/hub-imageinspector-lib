package com.synopsys.integration.blackduck.imageinspector.api;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "com.synopsys.integration.blackduck.imageinspector", "com.synopsys.integration.blackduck.imageinspector.lib", "com.google.gson" })
public class AppConfig {

}
