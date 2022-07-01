package net.just_s.sframes;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFramesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sframes");

	@Override
	public void onInitialize() {
		LOGGER.info("Seamless Frames loaded successfully!");
	}
}
