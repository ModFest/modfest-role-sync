package net.modfest.rolesync.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.gegy.roles.store.PlayerRoleManager;
import dev.gegy.roles.store.PlayerRoleSet;
import net.minecraft.server.MinecraftServer;
import net.modfest.rolesync.ModFestRoleSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(value = PlayerRoleManager.class, remap = false)
public class PlayerRolesPeek {
	@ModifyReturnValue(method = "peekRoles", at = @At("RETURN"))
	public PlayerRoleSet addModfestRoles(PlayerRoleSet original, MinecraftServer server, UUID uuid) {
		var role = ModFestRoleSync.getPlatformLookup().getRoleUUID(uuid);
		if (role != null) {
			var copy = new PlayerRoleSet(((RoleSetAccessor)(Object)original).getEveryoneRole(), null);
			copy.copyFrom(original);
			copy.add(role);
			return copy;
		}
		return original;
	}
}
