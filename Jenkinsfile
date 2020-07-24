#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([
		upstream('knime-expressions/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
	]),
	parameters(workflowTests.getConfigurationsAsParameters()),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.10"

try {
	// provide the name of the update site project
	knimetools.defaultTychoBuild('org.knime.update.base')
	

    workflowTests.runTests(
        dependencies: [
            repositories:  ["knime-base", "knime-expressions", "knime-core","knime-pmml", "knime-pmml-compilation",
            "knime-pmml-translation", "knime-r", "knime-jep","knime-kerberos", "knime-database", "knime-datageneration",
            "knime-filehandling", "knime-js-base", "knime-ensembles", "knime-distance", "knime-xml", "knime-jfreechart",
            "knime-timeseries", "knime-python", "knime-stats", "knime-h2o", "knime-weka", "knime-birt", "knime-svm",
            "knime-js-labs", "knime-optimization", "knime-streaming", "knime-textprocessing", "knime-chemistry", "knime-testing-internal",
            "knime-dl4j", "knime-exttool", "knime-parquet", "knime-bigdata", "knime-bigdata-externals", "knime-cloud", "knime-js-core",
            "knime-database-proprietary","knime-svg", "knime-excel"]
        ],
         sidecarContainers: [
            [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
         ]
    )

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
