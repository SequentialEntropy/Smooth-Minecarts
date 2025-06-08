package io.github.sequentialentropy;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothMinecarts implements ModInitializer {
	public static final String MOD_ID = "smooth-minecarts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Smooth Minecarts");
	}
}