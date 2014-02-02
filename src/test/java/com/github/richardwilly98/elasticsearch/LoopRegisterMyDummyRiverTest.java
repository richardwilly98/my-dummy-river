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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.get.GetResponse;
import org.testng.annotations.Test;

@Test
public class LoopRegisterMyDummyRiverTest extends MyDummyRiverTestAbstract {

    protected LoopRegisterMyDummyRiverTest() {
        super("my-dummy-river-" + System.currentTimeMillis(), "my-dummy-index-" + System.currentTimeMillis());
    }

    private void registerRiver(String index) {
        logger.debug("*** registerRiver ***");
        try {
            super.createRiver(TEST_MY_DUMMY_RIVER_JSON, (Object)index);
            Thread.sleep(1000);
        } catch (Throwable t) {
            logger.error("registerRiver failed.", t);
        }
    }

    private void cleanUp() {
        super.deleteRiver();
    }

    private void testRegisterRiver(String index) throws Throwable {
        logger.debug("Start testRiverStarted");
        try {
        	registerRiver(index);
        	assertThat(getNode().client().admin().indices().prepareExists("_river").get().isExists(), equalTo(true));
        	GetResponse response = getNode().client().prepareGet("_river", river, "_meta").get();
        	assertThat(response.isExists(), equalTo(true));
            ClusterHealthResponse clusterHealth = getNode().client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus())
                    .actionGet();
            logger.info("Done Cluster Health, status " + clusterHealth.getStatus());
        	assertThat(getNode().client().admin().indices().prepareExists(index).get().isExists(), equalTo(true));
        	Thread.sleep(1000);
        } catch (Throwable t) {
            logger.error("testRiverStarted failed.", t);
            t.printStackTrace();
            throw t;
        } finally {
        	cleanUp();
        }
    }
    
    @Test
    public void registerMultipleRivers() throws Throwable {
    	int max = 10;
    	int count = 0;
    	while (count < max) {
    		String index = "my-dummy-index";
    		testRegisterRiver(index);
    		count++;
    	}
    }

}
