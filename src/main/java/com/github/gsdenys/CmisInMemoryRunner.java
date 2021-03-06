/*
 * Copyright 2016 gsdenys@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.gsdenys;

import com.github.gsdenys.runner.base.CmisVersionDefinition;
import com.github.gsdenys.runner.base.CmisWarFinder;
import com.github.gsdenys.runner.base.DocumentTypeLoader;
import com.github.gsdenys.runner.base.PortDefinition;
import com.github.gsdenys.runner.type.creator.TypeCreator;
import com.github.gsdenys.runner.type.parser.ParserException;
import com.github.gsdenys.runner.utils.CmisUtils;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Start a CMIS In Memory with Jetty Server.
 *
 * @author Denys G. Santos (gsdenys@gmail.com)
 * @version 1.0.3
 * @since 0.0.1
 */
public class CmisInMemoryRunner extends BlockJUnit4ClassRunner {

    private static final String CMIS_JETTY_CONTEXT = "/cmis/";
    private static boolean initialized = false;
    private static Server server;
    private static CmisVersionDefinition versionDefinition;
    private static List<TypeDefinition> typeDefinitionList;

    private DocumentTypeLoader documentTypeLoader;

    /**
     * CMIS In Memory Starter
     *
     * @param clazz The class that will be use to sincronize
     * @throws InitializationError If any error cour
     */
    public CmisInMemoryRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

        versionDefinition = new CmisVersionDefinition(this);

        PortDefinition def = new PortDefinition(this);
        int portDefinedByProgramer = def.defineCmisServerPort();

        TypeCreator typeCreator = new TypeCreator();
        this.documentTypeLoader = new DocumentTypeLoader(this);

        this.preLoad(portDefinedByProgramer);

        //check the port availability
        if (!def.isPortAvailable(portDefinedByProgramer)) {
            throw new InitializationError("The port '" + portDefinedByProgramer + "' is not available to use.");
        }

        //initialize the server
        synchronized (clazz) {
            if (!initialized) {
                try {
                    initialized = this.startJettyServer(
                            new CmisWarFinder().getCmisWarPath(),
                            portDefinedByProgramer
                    );

                    typeDefinitionList = documentTypeLoader.load();

                    typeDefinitionList.forEach(typeCreator::execute);
                } catch (Exception e) {
                    throw new InitializationError(e);
                }
            }
        }


    }

    /**
     * Get the defineCmisServerPort where the CMIS server was started
     *
     * @return the CMIS server defineCmisServerPort
     */
    public static Integer getCmisPort() {
        String regex = "(.*:)(\\d+)(/.*)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(server.getURI().toString());

        String prt = matcher.find() ? matcher.group(2) : "0";

        return Integer.parseInt(prt);
    }

    /**
     * Get the CMIS URI
     *
     * @return URI the cmis uri
     */
    public static URI getCmisURI() {
        if (versionDefinition == null) {
            return URI.create(
                    server.getURI().toString().concat(
                            CmisVersionDefinition.CMIS_RELATIVE_URL_1_1
                    )
            );
        }

        return URI.create(server.getURI().toString().concat(versionDefinition.getCmisPath()));
    }

    /**
     * get the CMIS session from default repoistory (A1)
     *
     * @return Session the CMIS {@link Session}
     */
    public static Session getSession() {
        return CmisInMemoryRunner.getSession(null);
    }

    /**
     * Get the cmis Session from repository passed by parameter
     *
     * @param repositoryId the CMIS repository id
     * @return Session the CMIS {@link Session}
     */
    public static Session getSession(String repositoryId) {
        CmisUtils cmisUtils = new CmisUtils();
        return cmisUtils.getSession(repositoryId);
    }

    /**
     * Check if jetty restart is required
     *
     * @param portDefinedByProgramer the port defined by user
     * @return boolean <code>true</code> case restart is required
     */
    private boolean isRestartRequired(final int portDefinedByProgramer) {
        //case server is running
        if (server != null && server.isRunning()) {

            //case jetty was started at different port that defined at @Configure annotation (field port)
            if (getCmisPort() != portDefinedByProgramer) {
                return true;
            }

            //case CMIS has any no defined type that was configured at @Configure annotation (field docTypes)
            try {
                List<TypeDefinition> typeDef = this.documentTypeLoader.load();

                for (TypeDefinition td : typeDefinitionList) {
                    for (int i = 0; i < typeDef.size(); i++) {
                        TypeDefinition td2 = typeDef.get(i);
                        if (td.getId().equals(td2.getId())) {
                            typeDef.remove(i);
                            break;
                        }
                    }
                }

                if (typeDef.size() > 0) {
                    return true;
                }
            } catch (ParserException e) {
                e.printStackTrace();
            }

        }

        return false;
    }


    /**
     * Execute extra commands before start jetty server
     *
     * @param portDefinedByProgramer the port defined by user
     * @throws InitializationError when the stop jetty command fault
     */
    private void preLoad(final int portDefinedByProgramer) throws InitializationError {

        if (this.isRestartRequired(portDefinedByProgramer)) {

            initialized = false;

            try {
                server.stop();
            } catch (Exception e) {
                throw new InitializationError("Unable to stop and destroy the active instance of jetty  server");
            }
        }
    }

    /**
     * Start jetty server
     *
     * @param cmisWar the full path to the cmis in memory war file
     * @param port    the defineCmisServerPort to start jetty server
     * @return boolean
     * @throws Exception any exception that can occur
     */
    private boolean startJettyServer(final String cmisWar, final Integer port) throws Exception {
        server = new Server(port);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath(CmisInMemoryRunner.CMIS_JETTY_CONTEXT);
        webapp.setWar(cmisWar);

        server.setHandler(webapp);
        server.start();

        return true;
    }
}
