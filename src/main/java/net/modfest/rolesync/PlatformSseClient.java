package net.modfest.rolesync;

import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import org.jspecify.annotations.NonNull;

import java.net.http.HttpRequest;

public class PlatformSseClient extends SseClient {
	@Override
	public void onEvent(SseEvent sseEvent) {

	}

	@Override
	public void configureRequest(HttpRequest.@NonNull Builder builder) {

	}
}
