package com.safetychanger.libqueuesystem;

import android.support.annotation.NonNull;

import java.util.LinkedHashMap;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Created by niket.
 */
class QueueApiClient {

    //public static final String API_COLLECTOR_LOG = "/collector/logs/";

    /**
     * @param serverUrl
     * @param headers
     * @return
     */
    public static QueueApiInterface getIncidentAppApiClient(@NonNull String serverUrl, final @NonNull LinkedHashMap<String, String> headers) {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(serverUrl).setRequestInterceptor(new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (headers != null && headers.size() > 0) {
                    for (LinkedHashMap.Entry<String, String> header : headers.entrySet()) {
                        request.addHeader(header.getKey(), header.getValue());
                    }
                }
            }
        }).setLogLevel(RestAdapter.LogLevel.FULL).build();

        QueueApiInterface incidentApiClient = restAdapter.create(QueueApiInterface.class);

        return incidentApiClient;
    }
}
