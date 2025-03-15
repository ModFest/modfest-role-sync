package net.modfest.rolesync.platform;

import com.mojang.util.UndashedUuid;
import dev.gegy.roles.api.PlayerRolesApi;
import dev.gegy.roles.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.modfest.rolesync.ModFestRoleSync;
import net.modfest.rolesync.PlatformRoleLookup;
import net.modfest.rolesync.config.BehaviourConfig;
import net.modfest.rolesync.config.PlatformConfig;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class SyncedRoleLookup implements PlatformRoleLookup, Closeable {
	private final Role participantRole;
	private final Role teamMemberRole;
	private final PlatformSseClient sseClient;
	private final HashMap<UUID, Role> assignedRoles;

	public SyncedRoleLookup(BehaviourConfig behaviourConfig, PlatformConfig platformConfig) {
		var provider = PlayerRolesApi.provider();
		this.participantRole = provider.get(behaviourConfig.participantRole());
		this.teamMemberRole = provider.get(behaviourConfig.teamMemberRole());
		if (this.participantRole == null) {
			throw new RuntimeException("Couldn't find role named "+behaviourConfig.participantRole());
		}
		if (this.teamMemberRole == null) {
			throw new RuntimeException("Couldn't find role named "+behaviourConfig.teamMemberRole());
		}
		this.assignedRoles = new HashMap<>();
		this.sseClient = new PlatformSseClient(platformConfig) {
			@Override
			public void onDataUpdate(Collection<UserData> newData) {
				assignedRoles.clear();
				for (var user : newData) {
					for (var mcAcc : user.minecraft_accounts()) {
						var u = UndashedUuid.fromStringLenient(mcAcc);
						if (Objects.equals(user.role(), "team_member")) {
							assignedRoles.put(u, teamMemberRole);
						} else {
							var p = assignedRoles.get(u);
							if (p != null && p != teamMemberRole) {
								if (user.registered() != null && user.registered().contains(behaviourConfig.eventId())) {
									assignedRoles.put(u, participantRole);
								}
							}
						}
					}
				}
			}
		};
	}

	@Override
	public @Nullable Role getRole(PlayerEntity player) {
		return teamMemberRole;
//		return assignedRoles.get(player.getUuid());
	}

	@Override
	public @Nullable Role getRoleUUID(UUID id) {
		return assignedRoles.get(id);
	}

	@Override
	public void close() throws IOException {
		this.sseClient.close();
	}
}
