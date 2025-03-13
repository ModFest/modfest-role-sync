package net.modfest.rolesync.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigReader {
	private static <T> T read(Path location, Class<T> type, String defaultContent) throws IOException {
		if (!Files.exists(location)) {
			Files.createDirectories(location.getParent());
			Files.writeString(location, defaultContent);
		}
		var p = new Properties();
		p.load(Files.newBufferedReader(location));

		// We're hijacking gson's serialization capabilities
		var json = new JsonObject();
		p.forEach((key, value) -> {
			json.addProperty((String)key, (String)value);
		});
		return new Gson().fromJson(json, type);
	}

	public static BehaviourConfig readBehaviourConfig() {
		try {
			var configDir = FabricLoader.getInstance().getConfigDir();
			var behaviourPath = configDir.resolve("modfest_sync/behaviour.properties");
			return read(behaviourPath, BehaviourConfig.class, """
				# The event id which will be checked for in the platform data. Anyone
				# registered to an event which matches this id will be assigned a role
				eventId=...
				# The role which is assigned to participants. Must match a role inside of Player Roles' roles.json
				participantRole=myRole
				# The role which is assigned to team members. Must match a role inside of Player Roles' roles.json
				teamMemberRole=myRole
				""");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static PlatformConfig readPlatformConfig() {
		try {
			var configDir = FabricLoader.getInstance().getConfigDir();
			var platformPath = configDir.resolve("modfest_sync/platform.properties");
			return read(platformPath, PlatformConfig.class, """
				# The url to use for synchronizing data
				platformUrl=https://platform.modfest.net
				# The token used to authenticate. If this line is commented, role sync is disabled
				#platformToken=event_tkyO1TbWOyTQxbhh2V58DarYaj0TxqKHp6ARZihR2eyLH+8tOFULrtlEynfBoiz2r0ywR5F9ZFqa2cB88iAKGQ==
				""");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
