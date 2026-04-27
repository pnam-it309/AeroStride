package com.example.be;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("AeroStride Full Test Suite")
@SelectPackages("com.example.be")
@ExcludeClassNamePatterns(".*BeApplicationTests")
public class BeApplicationTests {
}
