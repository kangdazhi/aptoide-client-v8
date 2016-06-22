/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 04/05/2016.
 */

package cm.aptoide.pt.networkclient.okhttp;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import cm.aptoide.pt.networkclient.BuildConfig;
import cm.aptoide.pt.networkclient.okhttp.cache.RequestCache;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Factory for OkHttp Clients creation.
 *
 * @author Neurophobic Animal
 * @author SithEngineer
 */
public class OkHttpClientFactory {

	private static final String TAG = OkHttpClientFactory.class.getName();
	private static OkHttpClient httpClientInstance;

	public static OkHttpClient newClient(File cacheDirectory, int cacheMaxSize, Interceptor interceptor) {
		return new OkHttpClient.Builder()
				.cache(new Cache(cacheDirectory, cacheMaxSize)) // 10 MiB
				.addInterceptor(interceptor)
				.build();
	}

	public static OkHttpClient newClient() {
		return new OkHttpClient.Builder().build();
	}

	public static OkHttpClient getSingletoneClient() {
		if (httpClientInstance == null) {
			httpClientInstance = newClient(new File("/"), 10 * 1024 * 1024, new AptoideCacheInterceptor());
		}
		return httpClientInstance;
	}

	private static final class AptoideCacheInterceptor implements Interceptor {

		private final String TAG = OkHttpClientFactory.TAG + "." + AptoideCacheInterceptor.class
				.getSimpleName();

		private final RequestCache customCache = new RequestCache();

		@Override
		public Response intercept(Chain chain) throws IOException {
			Request request = chain.request();
			Response response = customCache.get(request);

			if (response != null) {

				if(BuildConfig.DEBUG) {
					Log.v(TAG, "cache hit: " + request.url());
				}

				return response;
			}

			if(BuildConfig.DEBUG) {
				Log.v(TAG, "cache miss: " + request.url());
			}

			Response cachedResponse = customCache.put(request, chain.proceed(request));
			return cachedResponse;
		}
	}
}
