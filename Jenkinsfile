#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-03'

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([
		upstream('knime-expressions/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
	]),
    parameters(workflowTests.getConfigurationsAsParameters() + fsTests.getFSConfigurationsAsParameters()),

	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"

try {
	// provide the name of the update site project
	knimetools.defaultTychoBuild('org.knime.update.base')

    testConfigs = [
        WorkflowTests: {
            workflowTests.runTests(
                dependencies: [
                    repositories:  ["knime-base", "knime-expressions", "knime-core", "knime-core-ui", "knime-pmml", "knime-pmml-compilation",
                    "knime-pmml-translation", "knime-r", "knime-jep","knime-kerberos", "knime-database", "knime-datageneration",
                    "knime-filehandling", "knime-js-base", "knime-ensembles", "knime-distance", "knime-xml", "knime-jfreechart",
                    "knime-timeseries", "knime-python", "knime-python-legacy", "knime-conda", "knime-stats", "knime-h2o", "knime-weka", "knime-birt", "knime-svm",
                    "knime-js-labs", "knime-optimization", "knime-streaming", "knime-textprocessing", "knime-chemistry", "knime-python", "knime-testing-internal",
                    "knime-exttool", "knime-parquet", "knime-bigdata", "knime-bigdata-externals", "knime-cloud", "knime-js-core", "knime-office365",
                    "knime-database-proprietary","knime-svg", "knime-excel", "knime-wide-data"],
                    ius: ["org.knime.features.chem.types.feature.group"]
                ],
                sidecarContainers: [
                    [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
                ]
            )
        },
        FileHandlingTests: {
            workflowTests.runFilehandlingTests (
                dependencies: [
                    repositories: [
                        "knime-base", "knime-core", "knime-expressions",
                        "knime-jep", "knime-datageneration", "knime-js-base",
                        "knime-js-core", "knime-r", "knime-database",
                        "knime-kerberos", "knime-timeseries",
                        "knime-jfreechart", "knime-distance", "knime-streaming"
                    ]
                ],
            )
        },
        'Integrated Workflowtests': {
            workflowTests.runIntegratedWorkflowTests(profile: 'test',  nodeType: 'maven', configurations: workflowTests.DEFAULT_FEATURE_BRANCH_CONFIGURATIONS)
         },
    ]

    parallel testConfigs

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
