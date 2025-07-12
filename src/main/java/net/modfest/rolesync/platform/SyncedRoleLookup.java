package net.modfest.rolesync.platform;

import com.mojang.util.UndashedUuid;
import dev.gegy.roles.api.PlayerRolesApi;
import dev.gegy.roles.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.modfest.rolesync.PlatformRoleLookup;
import net.modfest.rolesync.SyncedRole;
import net.modfest.rolesync.cache.CacheManager;
import net.modfest.rolesync.config.BehaviourConfig;
import net.modfest.rolesync.config.PlatformConfig;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SyncedRoleLookup implements PlatformRoleLookup, Closeable {
	private final Role participantRole;
	private final Role teamMemberRole;
	private final PlatformSseClient sseClient;
	private final HashMap<UUID, Role> assignedRoles;
	private final CacheManager cacheManager;
	private final Lock updateLock = new ReentrantLock();
	private long lastUpdated = Long.MIN_VALUE;

	public SyncedRoleLookup(BehaviourConfig behaviourConfig, PlatformConfig platformConfig, CacheManager cacheManager) {
		this.cacheManager = cacheManager;
		cacheManager.read((s) -> {
			this.updateRoles(() -> s, Long.MIN_VALUE + 1); // Set the time to -infinity, because this cache is dated before anything else which we'll find
		});
		var provider = PlayerRolesApi.provider();
		this.participantRole = provider.get(behaviourConfig.participantRole());
		this.teamMemberRole = provider.get(behaviourConfig.teamMemberRole());
		if (this.participantRole == null) {
			throw new RuntimeException("Couldn't find role named "+behaviourConfig.participantRole());
		}
		if (this.teamMemberRole == null) {
			throw new RuntimeException("Couldn't find role named "+behaviourConfig.teamMemberRole());
		}
		this.assignedRoles = new HashMap<>();
		this.sseClient = new PlatformSseClient(platformConfig) {
			@Override
			public void onDataUpdate(Collection<UserData> newData) {
				updateRoles(() -> newData.stream()
					.map(user -> {
						SyncedRole r;
						if (Objects.equals(user.role(), "team_member")) {
							r = SyncedRole.TEAM;
						} else if (user.registered() != null && user.registered().contains(behaviourConfig.eventId())) {
							r = SyncedRole.PARTICIPANT;
						} else {
							r = null;
						}
						return new Pair<>(user, r);
					})
					.flatMap(p ->
						p.getLeft().minecraft_accounts()
							.stream().map(uuidStr -> {
								try {
									return UndashedUuid.fromStringLenient(uuidStr);
								} catch (IllegalArgumentException ignored) {
									return null;
								}
							})
							.filter(Objects::nonNull)
							.map(uuid -> new Pair<>(uuid, p.getRight()))
				));
			}
		};
	}

	private void updateRoles(Supplier<Stream<Pair<UUID,SyncedRole>>> roleSupplier) {
		updateRoles(roleSupplier, System.nanoTime());
	}
	private void updateRoles(Supplier<Stream<Pair<UUID,SyncedRole>>> roleSupplier, long time) {
		updateLock.lock();
		try {
			if (lastUpdated > time) {
				return;
			}
			lastUpdated = time;
			assignedRoles.clear();
			roleSupplier.get().forEach(p -> {
				var uuid = p.getLeft();
				var role = p.getRight();
				switch (role) {
					case TEAM -> assignedRoles.put(uuid, teamMemberRole);
					case PARTICIPANT -> {
						if (assignedRoles.get(uuid) != teamMemberRole) {
							assignedRoles.put(uuid, participantRole);
						}
					}
					case null -> {}
				}
			});
		} finally {
			updateLock.unlock();
		}
	}

	@Override
	public @Nullable Role getRole(PlayerEntity player) {
		return assignedRoles.get(player.getUuid());
	}

	@Override
	public @Nullable Role getRoleUUID(UUID id) {
		return assignedRoles.get(id);
	}

	@Override
	public void close() throws IOException {
		this.sseClient.close();
		this.cacheManager.write(() -> this.assignedRoles.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue() == teamMemberRole ? SyncedRole.TEAM : SyncedRole.PARTICIPANT)));
	}
}
