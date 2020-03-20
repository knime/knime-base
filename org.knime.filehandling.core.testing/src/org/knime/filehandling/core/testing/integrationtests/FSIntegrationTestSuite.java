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
import org.knime.filehandling.core.testing.integrationtests.path.CompareTest;
import org.knime.filehandling.core.testing.integrationtests.path.EndsStartsWithTest;
import org.knime.filehandling.core.testing.integrationtests.path.PathTest;
import org.knime.filehandling.core.testing.integrationtests.path.SubPathTest;
import org.knime.filehandling.core.testing.integrationtests.path.ToStringTest;

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
	SubPathTest.class,
	PathTest.class,
	ToStringTest.class,
	EndsStartsWithTest.class,
	CompareTest.class,
	OutputStreamTest.class
})
public class FSIntegrationTestSuite {

}
