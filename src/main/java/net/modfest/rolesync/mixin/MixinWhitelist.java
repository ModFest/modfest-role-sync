package net.modfest.rolesync.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Whitelist.class)
public class MixinWhitelist {
	@Inject(method = "isAllowed", at = @At("HEAD"), cancellable = true)
	public void injectIsAllowed(GameProfile profile, CallbackInfoReturnable<Boolean> cir) {

	}
}
