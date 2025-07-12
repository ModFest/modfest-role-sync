package net.modfest.rolesync.cache;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.hash.HashingOutputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Pair;
import net.modfest.rolesync.ModFestRoleSync;
import net.modfest.rolesync.SyncedRole;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheManager {
	private static final long MAGIC = 0xBEBE220da001BEBEL;
	private final @NonNull Path location;
	private byte[] hash;

	public CacheManager() {
		this(FabricLoader.getInstance().getGameDir().resolve("platform_role_cache.bin"));
	}

	public CacheManager(@NonNull Path location) {
		this.location = location;
	}

	public void read(Consumer<Stream<Pair<UUID,SyncedRole>>> onReadFinished) {
		var t = new Thread(() -> {
			// We simultaneously read the file and compute its hash, for future reference
			var hash = Hashing.sha512();
			try (var hashStream = new HashingInputStream(hash, new BufferedInputStream(new FileInputStream(location.toFile())))) {
				var s = new DataInputStream(hashStream);
				var magic = s.readLong();
				if (magic != MAGIC) {
					ModFestRoleSync.LOGGER.warn("Cache file is invalid");
					return;
				}
				var len = s.readInt();
				var contents = new ArrayList<Pair<UUID,SyncedRole>>(len);

				for (int i = 0; i < len; i++) {
					var uuid = new UUID(s.readLong(), s.readLong());
					var b = s.readByte();
					var role = switch (b) {
						case 1 -> SyncedRole.TEAM;
						case 8 -> SyncedRole.PARTICIPANT;
						default -> throw new IOException("Invalid role in cache");
					};
					contents.add(new Pair<>(uuid, role));
				}
				// We only set the hash and call the callback if we successfully read the whole file.
				// It's possible for the file to be corrupt (especially since our writes aren't atomic),
				// we should be able to recover from that
				this.hash = hashStream.hash().asBytes();
				onReadFinished.accept(contents.stream());
			} catch (FileNotFoundException ignored) {

			} catch (IOException e) {
				ModFestRoleSync.LOGGER.warn("Couldn't read cache file", e);
			}
		});
		t.setName("ModfestRoleSync-Read");
		t.setDaemon(true);
		t.start();
	}

	public void write(Supplier<Stream<Pair<UUID,SyncedRole>>> provider) {
		// Run on a separate thread to not bog down the server with IO
		// This function should only be running whenever the server shuts down (or
		// something in the configuration changes), so there's little performance concern
		// in creating a new thread every time.
		// TODO we should probably ensure that there aren't two threads running at once
		var t = new Thread(() -> {
			// Ensure the list is consistent by sorting it by uuid
			var list = provider.get().collect(Collectors.toCollection(ArrayList::new));
			list.sort(Comparator.comparing(Pair::getLeft));
			// We write the collection to a hashing output first, and we only save the file if
			// the hash changes
			var hash = Hashing.sha512();
			try (var hashStream = new HashingOutputStream(hash, OutputStream.nullOutputStream())) {
				write(list, hashStream);
				if (Arrays.equals(this.hash, hashStream.hash().asBytes())) {
					ModFestRoleSync.LOGGER.info("Not writing cache as it hasn't changed");
				} else {
					// The hash changed! Write the actual file
					if (!Files.exists(this.location)) {
						Files.createFile(this.location);
					}
					try (var realStream = new BufferedOutputStream(new FileOutputStream(this.location.toFile()))) {
						write(list, realStream);
					}
				}
			} catch (IOException e) {
				ModFestRoleSync.LOGGER.warn("Couldn't write cache file", e);
			}
		});
		t.setName("ModfestRoleSync-Write");
		t.start();
	}

	private void write(List<Pair<UUID,SyncedRole>> data, OutputStream stream) throws IOException {
		var s = new DataOutputStream(stream);
		s.writeLong(MAGIC);
		s.writeInt(data.size());
		for (var p : data) {
			s.writeLong(p.getLeft().getMostSignificantBits());
			s.writeLong(p.getLeft().getLeastSignificantBits());
			s.writeByte(switch (p.getRight()) {
				case TEAM -> 1;
				case PARTICIPANT -> 8;
			});
		}
	}
}
