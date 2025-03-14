package net.modfest.rolesync;

import dev.gegy.roles.api.PlayerRolesApi;
import net.fabricmc.api.ModInitializer;
import net.modfest.rolesync.config.ConfigReader;
import net.modfest.rolesync.logging.MiniLogger;
import net.modfest.rolesync.logging.Slf4jLogger;
import net.modfest.rolesync.platform.SyncedRoleLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class ModFestRoleSync implements ModInitializer {
	public static final String MOD_ID = "modfest-role-sync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static WrappedLookup PLAYER_ROLES_LOOKUP = null;

	@Override
	public void onInitialize() {
		// Wrap player roles' lookup with ours
		var rootLookup = PlayerRolesApi.lookup();
		PLAYER_ROLES_LOOKUP = new WrappedLookup(rootLookup);
		PlayerRolesApi.setRoleLookup(PLAYER_ROLES_LOOKUP);
		init(new Slf4jLogger(LOGGER));
	}

	public static void init(MiniLogger logger) {
		var behaviourConfig = ConfigReader.readBehaviourConfig();
		var platformConfig = ConfigReader.readPlatformConfig();

		if (platformConfig.platformUrl() == null || platformConfig.platformToken() == null) {
			if (platformConfig.platformUrl() == null) {
				logger.info("ModFest Role Sync is disabled by config (no platform url set)");
			} else {
				logger.info("ModFest Role Sync is disabled by config (no token set)");
			}
			setPlatformLookup(PlatformRoleLookup.EMPTY);
			return;
		}

		try {
			var lookup = new SyncedRoleLookup(behaviourConfig, platformConfig);
			setPlatformLookup(lookup);
			return;
		} catch (RuntimeException e) {
			logger.error("Failed to setup role sync: "+e.getMessage());
			setPlatformLookup(PlatformRoleLookup.EMPTY);
			return;
		}
	}

	public static void setPlatformLookup(PlatformRoleLookup platformLookup) {
		if (PLAYER_ROLES_LOOKUP == null) {
			throw new IllegalStateException("Trying to set platform lookup before role sync was initialized");
		}
		if (PLAYER_ROLES_LOOKUP instanceof Closeable c) {
			try {
				c.close();
			} catch (IOException ignored) {}
		}
 		PLAYER_ROLES_LOOKUP.platformLookup = platformLookup;
	}
}
