val TestIntegration = config("test-integration").extend(Test)

configs(TestIntegration)

inConfig(TestIntegration)(Defaults.configSettings)

hoarder.withConfiguration(TestIntegration)

testRecompilationIn(Test, Compile, TestIntegration)