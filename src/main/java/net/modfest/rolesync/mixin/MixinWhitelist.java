package net.modfest.rolesync.mixin;

import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteList;
import net.modfest.rolesync.ModFestRoleSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UserWhiteList.class)
public class MixinWhitelist {
	@Inject(method = "isWhiteListed", at = @At("HEAD"), cancellable = true)
	public void injectIsAllowed(final NameAndId user, final CallbackInfoReturnable<Boolean> cir) {
		if (ModFestRoleSync.isWhitelist(user.id())) {
			cir.setReturnValue(true);
		}
	}
}
