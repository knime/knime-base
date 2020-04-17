package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class GetFileNameTest extends AbstractParameterizedFSTest {

    public GetFileNameTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testSimple() {
        assertEquals("0", getFileSystem().getPath("0").getFileName().toString());
    }

    @Test
    public void testSimple2() {
        assertEquals("1", getFileSystem().getPath("0", "1").getFileName().toString());
    }

    @Test
    public void testEmptyPath() {
        assertEquals(getFileSystem().getPath(""), getFileSystem().getPath("").getFileName());
    }
    
    @Test
    public void testDot() {
        assertEquals(getFileSystem().getPath("."), getFileSystem().getPath(".").getFileName());
    }
    
    @Test
    public void testDotSlash() {
        assertEquals(getFileSystem().getPath("./"), getFileSystem().getPath("./").getFileName());
    }
    
    @Test
    public void testDotDot() {
        assertEquals(getFileSystem().getPath(".."), getFileSystem().getPath("..").getFileName());
    }

    @Test
    public void testDotDotSlash() {
        assertEquals(getFileSystem().getPath("../"), getFileSystem().getPath("../").getFileName());
    }

    @Test
    public void testRoot() {
        assertEquals(null, getFileSystem().getRootDirectories().iterator().next().getFileName());
    }
}
