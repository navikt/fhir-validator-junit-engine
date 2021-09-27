## Run with JUnit Console Launcher

[Options](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher-options)

```
// First run: gradle shadowJar
java -jar junit.jar -cp build\libs\fhir-validator-junit-engine-0.1.0.jar:validator_cli.jar -d src\test\resources --reports-dir build\test-results
```
