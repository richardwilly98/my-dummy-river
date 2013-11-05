/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.richardwilly98.elasticsearch;

import static org.elasticsearch.client.Requests.clusterHealthRequest;
import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.PluginManager;
import org.elasticsearch.plugins.PluginManager.OutputMode;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public abstract class MyDummyRiverTestAbstract {

    public static final String TEST_MY_DUMMY_RIVER_JSON = "/com/github/richardwilly98/elasticsearch/test-my-dummy-river.json";

    protected final ESLogger logger = Loggers.getLogger(getClass());
    protected final static long wait = 500;

    private static Node node;
    private static Settings settings;

    protected final String river;
    protected final String index;

    protected MyDummyRiverTestAbstract(String river, String index) {
        this.river = river;
        this.index = index;
        loadSettings();
    }

    @BeforeSuite
    public void beforeSuite() throws Exception {
        logger.debug("*** beforeSuite ***");
        setupElasticsearchServer();
    }

    private void loadSettings() {
        settings = settingsBuilder().loadFromStream("settings.yml", ClassLoader.getSystemResourceAsStream("settings.yml")).build();
    }

    private void setupElasticsearchServer() throws Exception {
        logger.debug("*** setupElasticsearchServer ***");
        try {
            Tuple<Settings, Environment> initialSettings = InternalSettingsPreparer.prepareSettings(settings, true);
            if (!initialSettings.v2().configFile().exists()) {
                FileSystemUtils.mkdirs(initialSettings.v2().configFile());
            }

            if (!initialSettings.v2().logsFile().exists()) {
                FileSystemUtils.mkdirs(initialSettings.v2().logsFile());
            }

            if (!initialSettings.v2().pluginsFile().exists()) {
                FileSystemUtils.mkdirs(initialSettings.v2().pluginsFile());
                if (settings.getByPrefix("plugins") != null) {
                    PluginManager pluginManager = new PluginManager(initialSettings.v2(), null, OutputMode.DEFAULT);

                    Map<String, String> plugins = settings.getByPrefix("plugins").getAsMap();
                    for (String key : plugins.keySet()) {
                        pluginManager.downloadAndExtract(plugins.get(key));
                    }
                }
            } else {
                logger.info("Plugin {} has been already installed.", settings.get("plugins.mapper-attachments"));
                logger.info("Plugin {} has been already installed.", settings.get("plugins.lang-javascript"));
            }

            node = nodeBuilder().local(true).settings(settings).node();
        } catch (Exception ex) {
            logger.error("setupElasticsearchServer failed", ex);
            throw ex;
        }
    }

    protected String getJsonSettings(String jsonDefinition, Object... args) throws Exception {
        logger.debug("Get river setting");
        String setting = copyToStringFromClasspath(jsonDefinition);
        if (args != null) {
            setting = String.format(setting, args);
        }
        return setting;
    }

    protected void refreshIndex() {
        refreshIndex(index);
    }

    protected void refreshIndex(String index) {
        getNode().client().admin().indices().refresh(new RefreshRequest(index)).actionGet();
    }

    protected void createRiver(String jsonDefinition, String river, Object... args) throws Exception {
        logger.info("Create river [{}]", river);
        String setting = getJsonSettings(jsonDefinition, args);
        logger.info("River setting [{}]", setting);
        node.client().prepareIndex("_river", river, "_meta").setSource(setting).execute().actionGet();
        logger.debug("Running Cluster Health");
        ClusterHealthResponse clusterHealth = node.client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus())
                .actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.getStatus());
        GetResponse response = getNode().client().prepareGet("_river", river, "_meta").execute().actionGet();
        assertThat(response.isExists(), equalTo(true));
        refreshIndex("_river");
    }

    protected void createRiver(String jsonDefinition, Object... args) throws Exception {
        createRiver(jsonDefinition, river, args);
    }

    protected void deleteIndex(String name) {
        logger.info("Delete index [{}]", name);
        if (!node.client().admin().indices().prepareDelete(name).execute().actionGet().isAcknowledged()) {
            logger.error("Counld not delete index: {}. Try waiting 1 sec...", name);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Running Cluster Health");
        ClusterHealthResponse clusterHealth = node.client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus())
                .actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.getStatus());
    }

    protected void deleteIndex() {
        deleteIndex(index);
    }

    protected void deleteRiver() {
        deleteRiver(river);
    }

    protected void deleteRiver(String name) {
        logger.info("Delete river [{}]", name);
        node.client().admin().indices().prepareDeleteMapping("_river").setType(name).execute().actionGet();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Running Cluster Health");
        ClusterHealthResponse clusterHealth = node.client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus())
                .actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.getStatus());
    }

    @AfterSuite
    public void afterSuite() {
        logger.debug("*** afterSuite ***");
        shutdownElasticsearchServer();
    }

    private void shutdownElasticsearchServer() {
        logger.debug("*** shutdownElasticsearchServer ***");
        node.close();
    }

    protected static Node getNode() {
        return node;
    }

    protected String getRiver() {
        return river;
    }

    protected String getIndex() {
        return index;
    }
}
