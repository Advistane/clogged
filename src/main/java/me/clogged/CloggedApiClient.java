package me.clogged;

import okhttp3.*;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class CloggedApiClient {
    private static final String COLLECTION_LOG_API_HOST = "api.clogged.me";
    private static final String COLLECTION_LOG_USER_AGENT = "Runelite clogged/" + CloggedConfig.PLUGIN_VERSION;
    private static final MediaType COLLECTION_LOG_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private CloggedConfig config;

    public void updateUserCollectionLog(String requestBodyJson, Callback callback) {
        HttpUrl url = buildUrl();

        if (requestBodyJson == null || requestBodyJson.isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be null or empty");
        }

        try {
            RequestBody requestBody = RequestBody.create(COLLECTION_LOG_MEDIA_TYPE, requestBodyJson);
            Request request = createRequestBuilder(url)
                    .post(requestBody)
                    .build();

            apiRequest(request, callback);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request body", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send API request", e);
        }
    }

    public void getUserCollectionLog(String username, String subcategoryName, Callback callback) {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(username);
        pathSegments.add(subcategoryName);
        HttpUrl url = buildUrl(pathSegments);

        Request request = createRequestBuilder(url)
                .get()
                .build();

        apiRequest(request, callback);
    }

    public void getKCAliases(Callback callback) {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.add("kc-aliases");
        HttpUrl url = buildUrl(pathSegments);

        Request request = createRequestBuilder(url)
                .get()
                .build();

        apiRequest(request, callback);
    }


    private Request.Builder createRequestBuilder(HttpUrl url)
    {
        return new Request.Builder()
                .header("User-Agent", COLLECTION_LOG_USER_AGENT)
                .url(url);
    }

    private void apiRequest(Request request, Callback callback)
    {
        getOkHttpClient().newCall(request).enqueue(callback);
    }

    private OkHttpClient getOkHttpClient() {
        if (config.proxyEnabled()) {
            if (config.proxyHost() == null || config.proxyHost().isEmpty()) {
                throw new IllegalArgumentException("Proxy host cannot be null or empty");
            }
            if (config.proxyPort() <= 0) {
                throw new IllegalArgumentException("Proxy port must be a positive integer");
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.proxyHost(), config.proxyPort()));
            if (config.proxyUsername() != null && !config.proxyUsername().isEmpty()) {
                String proxyUsername = config.proxyUsername();
                String proxyPassword = config.proxyPassword() != null ? config.proxyPassword() : "";
                Authenticator proxyAuthenticator = (route, response) -> {
                    String credential = Credentials.basic(proxyUsername, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
                return okHttpClient.newBuilder()
                        .proxy(proxy)
                        .proxyAuthenticator(proxyAuthenticator)
                        .build();
            }

            return okHttpClient.newBuilder()
                    .proxy(proxy)
                    .build();
        }

        return okHttpClient;
    }

    private HttpUrl buildUrl() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host(COLLECTION_LOG_API_HOST)
                .addPathSegment("api")
                .addPathSegment("clog")
                .addPathSegment("update")
                .build();
    }

    private HttpUrl buildUrl(List<String> pathSegments) {
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("https")
                .host(COLLECTION_LOG_API_HOST)
                .addPathSegment("api")
                .addPathSegment("clog");

        for (String segment : pathSegments) {
            urlBuilder.addPathSegment(segment);
        }

        return urlBuilder.build();
    }
}
