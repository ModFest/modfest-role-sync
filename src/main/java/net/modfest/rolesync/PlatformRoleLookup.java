package net.modfest.rolesync;

import dev.gegy.roles.api.Role;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformRoleLookup {
	PlatformRoleLookup EMPTY = new PlatformRoleLookup() {
		@Override
		public @Nullable Role getRole(Player player) {
			return null;
		}

		@Override
		public @Nullable Role getRoleUUID(UUID id) {
			return null;
		}
	};

	@Nullable Role getRole(Player player);
	@Nullable Role getRoleUUID(UUID id);
}
