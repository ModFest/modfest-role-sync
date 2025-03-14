package net.modfest.rolesync.platform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class GsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
	private final TypeToken<T> type;
	private final Gson gson;
	private final HttpResponse.BodyHandler<InputStream> inner = HttpResponse.BodyHandlers.ofInputStream();

	public static <T> GsonBodyHandler<? extends List<T>> ofList(Class<T> type, Gson gson) {
		return new GsonBodyHandler<>((TypeToken<ArrayList<T>>)TypeToken.getParameterized(ArrayList.class, type), gson);
	}

	public GsonBodyHandler(TypeToken<T> type, Gson gson) {
		this.type = type;
		this.gson = gson;
	}

	public GsonBodyHandler(Class<T> type, Gson gson) {
		this(TypeToken.get(type), gson);
	}

	@Override
	public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
		return new GsonSubscriber(inner.apply(responseInfo));
	}

	private class GsonSubscriber implements HttpResponse.BodySubscriber<T>{
		private final HttpResponse.BodySubscriber<InputStream> inner;

		private GsonSubscriber(HttpResponse.BodySubscriber<InputStream> inner) {
			this.inner = inner;
		}

		@Override
		public CompletionStage<T> getBody() {
			return inner.getBody().thenApply(stream -> gson.fromJson(new InputStreamReader(stream), type));
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			inner.onSubscribe(subscription);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			inner.onNext(item);
		}

		@Override
		public void onError(Throwable throwable) {
			inner.onError(throwable);
		}

		@Override
		public void onComplete() {
			inner.onComplete();
		}
	}
}
