package net.modfest.rolesync;

import dev.gegy.roles.api.PlayerRolesApi;
import net.fabricmc.api.ModInitializer;
import net.modfest.rolesync.config.ConfigReader;
import net.modfest.rolesync.logging.MiniLogger;
import net.modfest.rolesync.logging.Slf4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModFestRoleSync implements ModInitializer {
	public static final String MOD_ID = "modfest-role-sync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static WrappedLookup PLAYER_ROLES_LOOKUP = null;

	@Override
	public void onInitialize() {
		// Wrap player roles' lookup with ours
		var rootLookup = PlayerRolesApi.lookup();
		PLAYER_ROLES_LOOKUP = new WrappedLookup(rootLookup);
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
	}

	public static void setPlatformLookup(PlatformRoleLookup platformLookup) {
		if (PLAYER_ROLES_LOOKUP == null) {
			throw new IllegalStateException("Trying to set platform lookup before role sync was initialized");
		}
		PLAYER_ROLES_LOOKUP.platformLookup = platformLookup;
	}
}
