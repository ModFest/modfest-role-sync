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
		var t = new Thread(() -> {
			var list = provider.get().collect(Collectors.toCollection(ArrayList::new));
			list.sort(Comparator.comparing(Pair::getLeft));
			var hash = Hashing.sha512();
			try (var hashStream = new HashingOutputStream(hash, OutputStream.nullOutputStream())) {
				write(list, hashStream);
				if (Arrays.equals(this.hash, hashStream.hash().asBytes())) {
					ModFestRoleSync.LOGGER.info("Not writing cache as it hasn't changed");
				} else {
					if (!Files.exists(this.location)) {
						Files.createFile(this.location);
					}
					try (var strem = new BufferedOutputStream(new FileOutputStream(this.location.toFile()))) {
						write(list, strem);
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
