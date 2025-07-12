package net.modfest.rolesync.platform;

import com.google.gson.Gson;
import net.modfest.rolesync.config.PlatformConfig;
import nl.theepicblock.sseclient.ReconnectionInfo;
import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;

import static net.modfest.rolesync.ModFestRoleSync.LOGGER;

public abstract class PlatformSseClient extends SseClient {
	private final Gson gson = new Gson();
	private PlatformConfig config;
	private URI platformUri;
	private HttpClient client;
	private final HashMap<String, UserData> localCache = new HashMap<>();

	public PlatformSseClient(PlatformConfig config) {
		this.config = config;
		this.client = HttpClient.newBuilder().build();
		try {
			this.platformUri = new URI(config.platformUrl());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		this.retryDelayMillis = 5_000L;
		this.connect();
	}

	public abstract void onDataUpdate(Collection<UserData> newData);

	@Override
	public void onConnect() {
		try {
			client.sendAsync(HttpRequest
						.newBuilder()
						.uri(platformUri.resolve("users"))
						.setHeader("Mf-Event-Token", config.platformToken())
						.setHeader("content-type", "application/json")
						.build(),
					GsonBodyHandler.<UserData>ofList(UserData.class, gson))
				.thenAccept(response -> {
					if (response.statusCode() != 200) {
						LOGGER.error("Failed to get users. Status {}", response.statusCode());
					} else {
						localCache.clear();
						for (var u : response.body()) {
							localCache.put(u.id(), u);
						}
						onDataUpdate(this.localCache.values());
						LOGGER.info("Platform connection successfully established");
					}
				}).exceptionally((t) -> {
					LOGGER.error("Unexpected error whilst processing user list", t);
					return null;
				});
			super.onConnect();
		} catch (Throwable t) {
			LOGGER.error("Failed to sync data", t);
		}
	}

	@Override
	public void onEvent(SseEvent sseEvent) {
		var updatedId = sseEvent.id;
		try {
			LOGGER.debug("Updating data for {}", updatedId);
			client.sendAsync(HttpRequest
						.newBuilder()
						.uri(platformUri.resolve("user/" + updatedId))
						.setHeader("Mf-Event-Token", config.platformToken())
						.setHeader("content-type", "application/json")
						.build(),
					new GsonBodyHandler<>(UserData.class, gson))
				.thenAccept(response -> {
					if (response.statusCode() != 200 && response.statusCode() != 404) {
						LOGGER.error("Failed to get user {}. Status {}", updatedId, response.statusCode());
					} else {
						if (response.statusCode() == 404) {
							localCache.remove(updatedId);
						} else {
							localCache.put(updatedId, response.body());
						}
						onDataUpdate(this.localCache.values());
					}
				}).exceptionally((t) -> {
					LOGGER.error("Unexpected error whilst processing user {}", updatedId, t);
					return null;
				});
		} catch (Throwable t) {
			LOGGER.error("Failed to update data for {}", updatedId, t);
		}
	}

	@Override
	protected @Nullable Duration onReconnect(@NonNull ReconnectionInfo reconnectionInfo) {
		if (reconnectionInfo.statusCode() != null && reconnectionInfo.statusCode() == 403) {
			LOGGER.error("Failed to subscribe to platform. Http status code 403");
			return null; // don't reconnect
		}
		// TODO some way of figuring out which exceptions are weird and which ones are simply timeouts.
		//      We need some logging!!
		var reconnTime = reconnectionInfo.wasConnectionInvalid() ?
			// Something is majorly wrong. Give the server some time
			Duration.ofMinutes(1)
		:
			// TODO need to expose the number of retries made in the SSE api
			Duration.ofMillis(retryDelayMillis);

		LOGGER.debug("Attempting to reconnect to platform SSE in {}. " +
			"(did connect = {}, " +
			"was valid = {}, " +
			"status code = {}, " +
			"error = {})",
			reconnTime,
			reconnectionInfo.connectionFailed(),
			reconnectionInfo.wasConnectionInvalid(),
			reconnectionInfo.statusCode(),
			reconnectionInfo.error());
		return reconnTime;
	}

	@Override
	public void onDisconnect() {
		LOGGER.debug("Disconnected from platform");
		super.onDisconnect();
	}

	@Override
	public void configureRequest(HttpRequest.@NonNull Builder builder) {
		builder
			.GET()
			.uri(platformUri.resolve("users/subscribe"))
			.setHeader("Mf-Event-Token", config.platformToken());
	}

	@Override
	public void close() {
		this.client.shutdownNow();
		super.close();
	}
}
