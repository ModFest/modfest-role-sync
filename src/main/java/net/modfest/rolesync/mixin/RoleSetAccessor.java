package net.modfest.rolesync.mixin;

import dev.gegy.roles.api.Role;
import dev.gegy.roles.store.PlayerRoleSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerRoleSet.class)
public interface RoleSetAccessor {
	@Accessor
	Role getEveryoneRole();
}
