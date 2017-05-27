val TestIntegration = config("test-integration").extend(Test)

configs(TestIntegration)

inConfig(TestIntegration)(Defaults.configSettings)

includeConfiguration(TestIntegration)

testRecompilationIn(Test, Compile, TestIntegration)