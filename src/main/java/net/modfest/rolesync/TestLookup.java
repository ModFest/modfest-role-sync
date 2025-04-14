package net.modfest.rolesync;

import dev.gegy.roles.api.PlayerRolesApi;
import dev.gegy.roles.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record TestLookup(Role role) implements PlatformRoleLookup {
	public TestLookup(String name) {
		this(PlayerRolesApi.provider().get(name));
	}

	@Override
	public @Nullable Role getRole(PlayerEntity player) {
		return role;
	}

	@Override
	public @Nullable Role getRoleUUID(UUID id) {
		return role;
	}
}
