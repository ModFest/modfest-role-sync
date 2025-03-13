package net.modfest.rolesync;

import dev.gegy.roles.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface PlatformRoleLookup {
	PlatformRoleLookup EMPTY = (player) -> null;

	@Nullable Role getRole(PlayerEntity player);
}
