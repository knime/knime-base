#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2024-06'

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([
		upstream('knime-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
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
                    repositories:  [
                        "knime-aws",
                        "knime-base",
                        "knime-bigdata",
                        "knime-bigdata-externals",
                        "knime-birt",
                        "knime-chemistry",
                        "knime-cloud",
                        "knime-conda",
                        "knime-core",
                        "knime-core-ui",
                        "knime-credentials-base",
                        "knime-database",
                        "knime-database-proprietary",
                        "knime-datageneration",
                        "knime-distance",
                        "knime-ensembles",
                        "knime-excel",
                        "knime-expressions",
                        "knime-exttool",
                        "knime-filehandling",
                        "knime-filehandling-core",
                        "knime-gateway",
                        "knime-h2o",
                        "knime-jep",
                        "knime-jfreechart",
                        "knime-js-base",
                        "knime-js-core",
                        "knime-js-labs",
                        "knime-kerberos",
                        "knime-office365",
                        "knime-optimization",
                        "knime-parquet",
                        "knime-pmml",
                        "knime-pmml-compilation",
                        "knime-pmml-translation",
                        "knime-python",
                        "knime-python-legacy",
                        "knime-r",
                        "knime-scripting-editor",
                        "knime-stats",
                        "knime-streaming",
                        "knime-svg",
                        "knime-svm",
                        "knime-testing-internal",
                        "knime-textprocessing",
                        "knime-timeseries",
                        "knime-weka",
                        "knime-wide-data",
                        "knime-xml"
                    ],
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
                        "knime-base",
                        "knime-core",
                        "knime-database",
                        "knime-datageneration",
                        "knime-distance",
                        "knime-expressions",
                        "knime-jep",
                        "knime-jfreechart",
                        "knime-js-base",
                        "knime-js-core",
                        "knime-kerberos",
                        "knime-r",
                        "knime-streaming",
                        "knime-timeseries",
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
