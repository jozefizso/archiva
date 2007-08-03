package org.apache.maven.archiva.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Test the configuration store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ArchivaConfigurationTest
    extends PlexusTestCase
{
    public void testGetConfigurationFromRegistryWithASingleNamedConfigurationResource()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "file://${appserver.base}/repositories/internal",
                      repository.getUrl() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );
    }

    public void testGetConfigurationFromDefaults()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-defaults" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "file://${appserver.base}/data/repositories/internal",
                      repository.getUrl() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );
    }

    private void assertConfiguration( Configuration configuration )
        throws Exception
    {
        FileTypes filetypes = (FileTypes) lookup( FileTypes.class.getName() );

        assertEquals( "check repositories", 4, configuration.getRepositories().size() );
        assertEquals( "check proxy connectors", 2, configuration.getProxyConnectors().size() );

        RepositoryScanningConfiguration repoScanning = configuration.getRepositoryScanning();
        assertNotNull( "check repository scanning", repoScanning );
        assertEquals( "check file types", 4, repoScanning.getFileTypes().size() );
        assertEquals( "check known consumers", 8, repoScanning.getKnownContentConsumers().size() );
        assertEquals( "check invalid consumers", 1, repoScanning.getInvalidContentConsumers().size() );

        List patterns = filetypes.getFileTypePatterns( "artifacts" );
        assertNotNull( "check 'artifacts' file type", patterns );
        assertEquals( "check 'artifacts' patterns", 13, patterns.size() );

        DatabaseScanningConfiguration dbScanning = configuration.getDatabaseScanning();
        assertNotNull( "check database scanning", dbScanning );
        assertEquals( "check unprocessed consumers", 6, dbScanning.getUnprocessedConsumers().size() );
        assertEquals( "check cleanup consumers", 3, dbScanning.getCleanupConsumers().size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        assertTrue( "check showFindArtifacts", ui.isShowFindArtifacts() );
        assertTrue( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

    public void testGetConfigurationFromRegistryWithTwoConfigurationResources()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration-both" );

        Configuration configuration = archivaConfiguration.getConfiguration();

        // from base
        assertEquals( "check repositories", 4, configuration.getRepositories().size() );
        // from user
        assertEquals( "check proxy connectors", 2, configuration.getProxyConnectors().size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        // from base
        assertFalse( "check showFindArtifacts", ui.isShowFindArtifacts() );
        // from user
        assertFalse( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

    public void testGetConfigurationSystemOverride()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-configuration" );

        System.setProperty( "org.apache.maven.archiva.webapp.ui.appletFindEnabled", "false" );

        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();

            assertFalse( "check boolean", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
        finally
        {
            System.getProperties().remove( "org.apache.maven.archiva.webapp.ui.appletFindEnabled" );
        }
    }

    public void testStoreConfiguration()
        throws Exception
    {
        File file = getTestFile( "target/test/test-file.xml" );
        file.delete();
        assertFalse( file.exists() );

        // TODO: remove with commons-configuration 1.4
        file.getParentFile().mkdirs();
        FileUtils.fileWrite( file.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save" );

        Configuration configuration = new Configuration();
        configuration.setVersion( "1" );
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", file.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        // read it back
        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-read-saved" );
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationUser()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        userFile.getParentFile().mkdirs();
        FileUtils.fileWrite( userFile.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check file not created", baseFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationLoadedFromDefaults()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", userFile.exists() );
        assertFalse( "Check file not created", baseFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testDefaultUserConfigFilename()
        throws Exception
    {
        DefaultArchivaConfiguration archivaConfiguration =
            (DefaultArchivaConfiguration) lookup( ArchivaConfiguration.class.getName() );

        assertEquals( System.getProperty( "user.home" ) + "/.m2/archiva.xml",
                      archivaConfiguration.getFilteredUserConfigFilename() );
    }

    public void testStoreConfigurationFallback()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.fileWrite( baseFile.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = new Configuration();
        configuration.setWebapp( new WebappConfiguration() );
        configuration.getWebapp().setUi( new UserInterfaceOptions() );
        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        archivaConfiguration.save( configuration );

        assertTrue( "Check file exists", baseFile.exists() );
        assertFalse( "Check file not created", userFile.exists() );

        // check it
        configuration = archivaConfiguration.getConfiguration();
        assertFalse( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
    }

    public void testStoreConfigurationFailsWhenReadFromBothLocations()
        throws Exception
    {
        File baseFile = getTestFile( "target/test/test-file.xml" );
        baseFile.delete();
        assertFalse( baseFile.exists() );

        File userFile = getTestFile( "target/test/test-file-user.xml" );
        userFile.delete();
        assertFalse( userFile.exists() );

        baseFile.getParentFile().mkdirs();
        FileUtils.fileWrite( baseFile.getAbsolutePath(), "<configuration/>" );

        userFile.getParentFile().mkdirs();
        FileUtils.fileWrite( userFile.getAbsolutePath(), "<configuration/>" );

        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-save-user" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );

        configuration.getWebapp().getUi().setAppletFindEnabled( false );

        try
        {
            archivaConfiguration.save( configuration );
            fail( "Configuration saving should not succeed if it was loaded from two locations" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            // check it was reverted
            configuration = archivaConfiguration.getConfiguration();
            assertTrue( "check value", configuration.getWebapp().getUi().isAppletFindEnabled() );
        }
    }

    public void testConfigurationUpgradeFrom09()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-upgrade-09" );

        // we just use the defaults when upgrading from 0.9 at this point.
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 0, configuration.getNetworkProxies().size() );

        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "file://${appserver.base}/data/repositories/internal",
                      repository.getUrl() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );
    }

    public void testAutoDetectV1()
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "test-autodetect-v1" );

        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration );
        assertEquals( "check network proxies", 1, configuration.getNetworkProxies().size() );

        RepositoryConfiguration repository =
            (RepositoryConfiguration) configuration.getRepositories().iterator().next();

        assertEquals( "check managed repositories", "file://${appserver.base}/repositories/internal",
                      repository.getUrl() );
        assertEquals( "check managed repositories", "Archiva Managed Internal Repository", repository.getName() );
        assertEquals( "check managed repositories", "internal", repository.getId() );
        assertEquals( "check managed repositories", "default", repository.getLayout() );
        assertTrue( "check managed repositories", repository.isIndexed() );
    }
}
