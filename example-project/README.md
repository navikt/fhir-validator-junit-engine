# Example IG project with tests

Make sure JDK (>= 1.8) is installed and add the following to the root of this project:
1. validator_cli.jar
2. junit-platform-console-standalone-1.8.1.jar
3. fhir-validator-junit-engine.jar

Open vscode and install the recommended *redhat.vscode-yaml* if you want intellisense in test-specification yaml files. Run the task already created in [.vscode/tasks.json](.vscode/tasks.json) by clicking `Terminal -> Run Task -> Run FHIR Validator JUnit tests`.
