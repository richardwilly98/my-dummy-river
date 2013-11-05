package com.github.richardwilly98.elasticsearch;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.river.RiversModule;

public class MyDummyRiverPlugin extends AbstractPlugin {

	public String name() {
		return "my-dummy-river";
	}

	public String description() {
		return "My Dummy River";
	}

    public void onModule(RiversModule module) {
        module.registerRiver("my-dummy-river", MyDummyRiverModule.class);
    }
}
