package com.netease.qa.testng;

import org.testng.reporters.XMLReporterConfig;

public class NeXMLReporterConfig extends XMLReporterConfig {
	
	public static final String ATTR_TC_NAME = "testName";
	public static final String ATTR_TC_SUITES = "suiteName";
	public static final String ATTR_AUTHOR = "author";
	public static final int	STACKTRACE_SHORT = 1;
	public static final int	STACKTRACE_FULL = 2;

}
