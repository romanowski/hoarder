val TestIntegration = config("test-integration").extend(Test)

configs(TestIntegration)

inConfig(TestIntegration)(Defaults.configSettings)

org.romanowski.HoarderSettings.includeConfiguration(TestIntegration)

org.romanowski.hoarder.tests.PluginTests.testRecompilationIn(Test, Compile, TestIntegration)