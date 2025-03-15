package net.modfest.rolesync.mixin;

import dev.gegy.roles.PlayerRoles;
import dev.gegy.roles.command.RoleCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.modfest.rolesync.ModFestRoleSync;
import net.modfest.rolesync.logging.CommandLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RoleCommand.class, remap = false)
public class MixinPlayerRolesReload {
	@Inject(method = "reloadRoles", at = @At("RETURN"))
	private static void onRoleReload(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
		source.getServer().execute(() -> {
			ModFestRoleSync.init(new CommandLogger(source));
		});
	}
}
