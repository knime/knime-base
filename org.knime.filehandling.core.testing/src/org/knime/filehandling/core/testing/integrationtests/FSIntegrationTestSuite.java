package org.knime.filehandling.core.testing.integrationtests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.ByteChannelTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.CheckAccessTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.CopyTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.CreateTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.DeleteTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.DirectoryStreamTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.InputStreamTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.MoveTest;
import org.knime.filehandling.core.testing.integrationtests.filesystemprovider.OutputStreamTest;
import org.knime.filehandling.core.testing.integrationtests.path.PathTest;

@RunWith(Suite.class)
@SuiteClasses({
	ByteChannelTest.class,
	CheckAccessTest.class,
	CopyTest.class,
	CreateTest.class,
	DeleteTest.class,
	DirectoryStreamTest.class,
	InputStreamTest.class,
	MoveTest.class,
	PathTest.class,
	OutputStreamTest.class
})
public class FSIntegrationTestSuite {

}
