package net.modfest.rolesync;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import dev.gegy.roles.api.Role;
import dev.gegy.roles.api.RoleLookup;
import dev.gegy.roles.api.RoleReader;
import dev.gegy.roles.api.override.RoleOverrideReader;
import dev.gegy.roles.api.override.RoleOverrideType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Wraps Player Roles' lookup and adds any of our own defined roles to it
 */
public class WrappedLookup implements RoleLookup {
	private final RoleLookup root;
	public @NotNull PlatformRoleLookup platformLookup;

	public WrappedLookup(RoleLookup root) {
		this.root = root;
		this.platformLookup = PlatformRoleLookup.EMPTY;
	}

	@Override
	public @NotNull RoleReader byPlayer(PlayerEntity player) {
		var additionalRole = this.platformLookup.getRole(player);
		if (additionalRole != null) {
			return new MergedRoleReader(additionalRole, root.byPlayer(player));
		}
		return root.byPlayer(player);
	}

	@Override
	public @NotNull RoleReader byEntity(Entity entity) {
		if (entity instanceof PlayerEntity player) {
			var additionalRole = this.platformLookup.getRole(player);
			if (additionalRole != null) {
				return new MergedRoleReader(additionalRole, root.byEntity(entity));
			}
		}
		return root.byEntity(entity);
	}

	@Override
	public @NotNull RoleReader bySource(ServerCommandSource serverCommandSource) {
		var entity = serverCommandSource.getEntity();
		if (entity instanceof PlayerEntity player) {
			var additionalRole = this.platformLookup.getRole(player);
			if (additionalRole != null) {
				return new MergedRoleReader(additionalRole, root.byEntity(entity));
			}
		}
		return root.bySource(serverCommandSource);
	}

	/**
	 * Extends a {@link RoleReader} with an additional role
	 */
	private record MergedRoleReader(@NotNull Role additional, @NotNull RoleReader root) implements RoleReader {
		@Override
		public boolean has(Role role) {
			return role == additional || root.has(role);
		}

		@Override
		public RoleOverrideReader overrides() {
			var ogReader = root.overrides();
			var additionalReader = additional.getOverrides();
			return new RoleOverrideReader() {
				@Override
				public @Nullable <T> Collection<T> getOrNull(RoleOverrideType<T> roleOverrideType) {
					var ogCollection = ogReader.getOrNull(roleOverrideType);
					if (ogCollection == null) {
						return additionalReader.getOrNull(roleOverrideType);
					} else {
						var l = new ArrayList<>(ogCollection);
						var a = additionalReader.getOrNull(roleOverrideType);
						if (a != null) {
							l.addAll(a);
						}
						return l;
					}
				}

				@Override
				public Set<RoleOverrideType<?>> typeSet() {
					var s = new HashSet<>(ogReader.typeSet());
					s.addAll(additionalReader.typeSet());
					return s;
				}
			};
		}

		@Override
		public @NotNull Iterator<Role> iterator() {
			return Iterators.concat(Iterators.singletonIterator(additional), root.iterator());
		}

		@Override
		public Stream<Role> stream() {
			return Streams.concat(Stream.of(additional), root.stream());
		}
	}
}
