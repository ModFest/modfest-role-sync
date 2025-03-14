package net.modfest.rolesync;

import dev.gegy.roles.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformRoleLookup {
	PlatformRoleLookup EMPTY = new PlatformRoleLookup() {
		@Override
		public @Nullable Role getRole(PlayerEntity player) {
			return null;
		}

		@Override
		public @Nullable Role getRoleUUID(UUID id) {
			return null;
		}
	};

	@Nullable Role getRole(PlayerEntity player);
	@Nullable Role getRoleUUID(UUID id);
}
