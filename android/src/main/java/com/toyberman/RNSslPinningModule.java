package com.toyberman;

import android.support.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.network.ForwardingCookieHandler;
import com.toyberman.Utils.OkHttpUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.cert.CertificateException;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RNSslPinningModule extends ReactContextBaseJavaModule {

    private static final String OPT_TAG = "tag";
    private static final String OPT_TRUST_KEY = "trust";

    private final ReactApplicationContext reactContext;
    private final HashMap<String, List<Cookie>> cookieStore;
    private CookieJar cookieJar = null;
    private ForwardingCookieHandler cookieHandler;
    private OkHttpClient client = null;

    public RNSslPinningModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        cookieStore = new HashMap<>();
        cookieHandler = new ForwardingCookieHandler(reactContext);
        cookieJar = new CookieJar() {

            @Override
            public synchronized void saveFromResponse(HttpUrl url, List<Cookie> unmodifiableCookieList) {
                for (Cookie cookie : unmodifiableCookieList) {
                    setCookie(url, cookie);
                }
            }

            @Override
            public synchronized List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }

            public void setCookie(HttpUrl url, Cookie cookie) {

                final String host = url.host();

                List<Cookie> cookieListForUrl = cookieStore.get(host);
                if (cookieListForUrl == null) {
                    cookieListForUrl = new ArrayList<Cookie>();
                    cookieStore.put(host, cookieListForUrl);
                }
                try {
                    putCookie(url, cookieListForUrl, cookie);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void putCookie(HttpUrl url, List<Cookie> storedCookieList, Cookie newCookie)
                    throws URISyntaxException, IOException {

                Cookie oldCookie = null;
                Map<String, List<String>> cookieMap = new HashMap<>();

                for (Cookie storedCookie : storedCookieList) {

                    // create key for comparison
                    final String oldCookieKey = storedCookie.name() + storedCookie.path();
                    final String newCookieKey = newCookie.name() + newCookie.path();

                    if (oldCookieKey.equals(newCookieKey)) {
                        oldCookie = storedCookie;
                        break;
                    }
                }
                if (oldCookie != null) {
                    storedCookieList.remove(oldCookie);
                }
                storedCookieList.add(newCookie);

                cookieMap.put("Set-cookie", Collections.singletonList(newCookie.toString()));
                cookieHandler.put(url.uri(), cookieMap);
            }
        };

    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    @ReactMethod
    public void getCookies(String domain, final Promise promise) {
        try {
            WritableMap map = new WritableNativeMap();

            List<Cookie> cookies = cookieStore.get(getDomainName(domain));

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    map.putString(cookie.name(), cookie.value());
                }
            }

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void removeCookieByName(String cookieName, final Promise promise) {
        List<Cookie> cookies = null;

        for (String domain : cookieStore.keySet()) {
            List<Cookie> newCookiesList = new ArrayList<>();

            cookies = cookieStore.get(domain);
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (!cookie.name().equals(cookieName)) {
                        newCookiesList.add(cookie);
                    }
                }
                cookieStore.put(domain, newCookiesList);
            }
        }

        promise.resolve(null);
    }

    @ReactMethod
    public void SSLPinning(String hostname, ReadableMap options, Promise promise) {
        try {
            // With ssl pinning
            if (options.hasKey(OPT_TRUST_KEY)) {
                if (options.getBoolean(OPT_TRUST_KEY) == true) {
                    client = getUnsafeOkHttpClient();
                }
            } else if (options.hasKey("certs")) {
                ReadableArray certs = options.getArray("certs");
                if (certs.size() == 0) {
                    // no ssl pinning
                    client = getUnsafeOkHttpClient();
                } else if (certs != null) {
                    client = OkHttpUtils.buildOkHttpClient(cookieJar, hostname, certs, options);
                }
            }
        } catch (Exception e) {
            promise.reject("Network Error", e);
        }
    }

    @ReactMethod
    public void fetch(final String hostname, final ReadableMap options, final Promise promise) {
        final ReactApplicationContext reactContext = this.reactContext;
        new Thread(new Runnable() {
            public void run() {
                try {
                    final WritableMap response = Arguments.createMap();
                    // With ssl pinning
                    if (client == null) {
                        OkHttpClient.Builder builder = new OkHttpClient.Builder();
                        client = builder.build();
                    }

                    Request request = OkHttpUtils.buildRequest(reactContext, options, hostname);

                    client.newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            promise.reject("Network Error", e);

                        }

                        @Override
                        public void onResponse(Call call, Response okHttpResponse) throws IOException {
                            String stringResponse = okHttpResponse.body().string();
                            // build response headers map
                            WritableMap headers = buildResponseHeaders(okHttpResponse);
                            // set response status code
                            response.putInt("status", okHttpResponse.code());
                            response.putString("data", stringResponse);
                            response.putMap("headers", headers);
                            response.putString("statusText", okHttpResponse.message());

                            promise.resolve(response);
                        }
                    });

                } catch (JSONException e) {
                    promise.reject("Network Error", e);
                }
            }
        }).start();

    }

    @ReactMethod
    public void cancel(final String tag) {
        for (Call call : client.dispatcher().queuedCalls()) {
            if (call.request().tag().equals(tag))
                call.cancel();
        }
        for (Call call : client.dispatcher().runningCalls()) {
            if (call.request().tag().equals(tag))
                call.cancel();
        }
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private WritableMap buildResponseHeaders(Response okHttpResponse) {
        Headers responseHeaders = okHttpResponse.headers();
        Set<String> headerNames = responseHeaders.names();
        WritableMap headers = Arguments.createMap();
        for (String header : headerNames) {
            headers.putString(header, responseHeaders.get(header));
        }
        return headers;
    }

    @Override
    public String getName() {
        return "RNSslPinning";
    }

}
