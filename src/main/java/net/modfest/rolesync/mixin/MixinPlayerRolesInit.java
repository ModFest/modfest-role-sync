package net.modfest.rolesync.mixin;

import dev.gegy.roles.PlayerRoles;
import net.modfest.rolesync.ModFestRoleSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRoles.class, remap = false)
public class MixinPlayerRolesInit {
	@Inject(method = "onInitialize", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		ModFestRoleSync.onPlayerRolesInit();
	}
}
