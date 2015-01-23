package com.safetychanger.libqueuesystem;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.safetychanger.libqueuesystem.utils.DebugLog;
import com.safetychanger.libqueuesystem.utils.Utils;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.protocol.HTTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import nl.changer.android.http.HttpHeader;
import retrofit.client.Response;

/**
 * Created by niket on 10/12/14.
 */
public class JobProcessor {

    private static final String TAG = JobProcessor.class.getSimpleName();
    public static final int STATUS_READY_TO_BE_UPLOADED = 0;
    public static final int STATUS_IN_PROCESS = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_PARTIAL_SUCCESS = 4;

    public static final int ERROR_NO_INTERNET_CONNECTION = 111;
    public static final int ERROR_NO_WIFI = 222;

    public static final String DATA_TYPE_IMAGE = "image";
    public static final String DATA_TYPE_VIDEO = "video";
    public static final String DATA_TYPE_AUDIO = "audio";
    public static final String DATA_TYPE_JSON = "json";

    private ArrayList<Job> mJobList = null;
    private int mUploadCount = 3;
    private JobDbManager mQueueDbManager;
    private OnJobProcess mOnJobProcess;
    boolean isUploadingInProgress = false;
    private Context mContext;
    private LinkedHashMap<String, String> mTempHeadersList;
    private static ArrayList<String> mRunningJobs = new ArrayList<String>();

    public JobProcessor() {
    }

    public JobProcessor(@NonNull Context context, @NonNull JobDbManager queueDbManager, OnJobProcess onJobProcess) {
        mContext = context;
        mQueueDbManager = queueDbManager;
        mOnJobProcess = onJobProcess;
    }

    public boolean isUploadingInProgress() {
        return isUploadingInProgress;
    }

    public void startUploading() {
        if (Utils.isInternetAvailable(mContext)) {
            // if (Utils.getDataConnectionType(mContext) == ConnectivityManager.TYPE_WIFI) {
            if (!isUploadingInProgress || mJobList == null || mJobList.size() == 0) {
                DebugLog.d(">>> Upload process started");
                allocateSlotToLogs(mUploadCount);
                isUploadingInProgress = true;
            } else {
                DebugLog.d(">>> call refused as process is already running");
            }
            /*} else {
                if (mOnJobProcess != null) {
                    mOnJobProcess.failure("no wifi connection", ERROR_NO_WIFI, null);
                }
            }*/
        } else {
            if (mOnJobProcess != null) {
                mOnJobProcess.failure("no internet connection", ERROR_NO_INTERNET_CONNECTION, null);
            }
        }
    }

    private void allocateSlotToLogs(int emptySlotCount) {
        synchronized (this) {
            if (mJobList != null && mJobList.size() > 0) {
                for (int i = 0; i < emptySlotCount; i++) {
                    // compare data here
                    if (mJobList != null && mJobList.size() > 0) {
                        if (!mRunningJobs.contains(mJobList.get(0).mId + mJobList.get(0).mJobType)) {
                            startUploading(i, mJobList.get(0));
                            mRunningJobs.add(mJobList.get(0).mId + mJobList.get(0).mJobType);
                        }
                        mJobList.remove(0);
                    } else {
                        break;
                    }
                }

            } else {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
                mJobList = mQueueDbManager.getAllJobBy(0);
                if (mJobList != null && mJobList.size() > 0) {
                    allocateSlotToLogs(mUploadCount);
                } else {
                    if (mOnJobProcess != null) {
                        isUploadingInProgress = false;
                        mOnJobProcess.allJobsFinished(">>> here we finished all jobs");
                    }

                }
//                        return;
//                    }
//                }).start();
            }
        }
    }

    private void startUploading(final int slotId, final Job job) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DebugLog.d(">>> Thread id: " + slotId + " start uploading: " + job.mId);

                    switch (job.mDataTpe) {
                        case DATA_TYPE_JSON:
                            postJson(job);
                            break;
                        case DATA_TYPE_AUDIO:
                        case DATA_TYPE_VIDEO:
                        case DATA_TYPE_IMAGE:
                        default:
                            uploadMedia(job);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }).start();
    }

    private void postJson(Job job) {
        try {

            /*if(Looper.getMainLooper().getThread() == Thread.currentThread()) {
                DebugLog.d("DDDDDDDDDDDDDDDDDDDD Main thread");
            }else{
                DebugLog.d("DDDDDDDDDDDDDDDDDDDD New thread");
            }*/

            JsonParser parser = new JsonParser();
            JsonElement jsonElement = (JsonElement) parser.parse(job.mData);

            if (mTempHeadersList != null && mTempHeadersList.size() > 0) {
                for (String key : mTempHeadersList.keySet()) {
                    String value = mTempHeadersList.get(key);
                    job.mHeadersList.put(key, value);
                }
            }

            Response response = QueueApiClient.getIncidentAppApiClient(job.mUrl, job.mHeadersList).postLog(jsonElement);
            if (mOnJobProcess != null) {
                if (response.getStatus() == 200) {
                    mQueueDbManager.updateJobStatus(job.mId, job.mDataTpe, STATUS_SUCCESS);
                    DebugLog.d("-----------------Logs uploaded successfully----------------------");

                    mOnJobProcess.success(getRetroFitResponse(response), response.getStatus(), job);
                } else {
                    mQueueDbManager.updateJobStatus(job.mId, job.mDataTpe, STATUS_FAILED);

                    mOnJobProcess.failure(getRetroFitResponse(response), response.getStatus(), job);
                }
            }
            slotIsEmpty(job);
        } catch (Exception e) {
            e.printStackTrace();
            if (mOnJobProcess != null) {
                mOnJobProcess.failure("End of file exception", -1, job);
            }
        }
    }

    private String getRetroFitResponse(Response response) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void slotIsEmpty(Job job) {
        /*synchronized (this) {
            if (mEmptySlotCount < mUploadCount) {
                mEmptySlotCount++;
            } else {
                allocateSlotToLogs(mUploadCount);
                mEmptySlotCount = 0;
            }
        }*/
        if (mRunningJobs.contains(job.mId + job.mJobType)) {
            mRunningJobs.remove(job.mId + job.mJobType);
        }
        allocateSlotToLogs(1);

    }

    private void uploadMedia(Job job) {
        DebugLog.d("inside upload media");
        try {
            //if (handleFileUpload(job.mId, job.mUrl, HttpPost.METHOD_NAME, Uri.parse(job.mData.toString()), job.mDataTpe)) {
            if (uploadObjectByUri(mContext, job.mUrl, job.mDataTpe, Uri.parse(job.mData.toString()))) {
                //if (AWSUploader.uploadObjectByUri(mContext, job.mUrl, job.mDataTpe, Uri.parse(job.mData), null)) {
                DebugLog.d("-----------------Attachment uploaded successfully----------------------");
                mQueueDbManager.updateJobStatus(job.mId, job.mDataTpe, STATUS_SUCCESS);
                mOnJobProcess.success("media uploaded success", 200, job);
            } else {
                mOnJobProcess.failure("media failed to upload", -1, job);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        slotIsEmpty(job);
    }

    private static final int MAX_BUFFER_SIZE = 1024 * 2;

    /**
     * *
     * This is the most efficient method to upload the data to server.
     * Prefer using this method over using the other ones in future.
     *
     * @param ctx
     * @param urlStr      Url to upload the data to
     * @param contentType
     * @param uri         {@link android.net.Uri} from where to get the data
     *                    **
     */
    public static boolean uploadObjectByUri(Context ctx, String urlStr, String contentType, Uri uri) {

        if (uri == null) {
            throw new NullPointerException("uri to upload cannot be null");
        }

        boolean isSucccessful = false;

        String path = Utils.getPathForMediaUri(ctx, uri);
        URL url = null;
        File f = new File(path);
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
        connection.setRequestProperty(HttpHeader.ACCEPT, "*/*");
        connection.setFixedLengthStreamingMode((int) f.length());

        Log.i(TAG, "#uploadObjectByUri <====Uploading... File length " + Utils.formatSize(f.length()) + " ====>");

        try {
            connection.setRequestMethod(HttpPut.METHOD_NAME);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        BufferedOutputStream out = null;
        int bytesRead = 0, bytesAvailable = 0, bufferSize;
        byte[] buffer;

        try {
            out = new BufferedOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (out != null) {
            FileInputStream fileInputStream = null;
            BufferedInputStream buffInputStream = null;
            try {
                fileInputStream = new FileInputStream(f);
                buffInputStream = new BufferedInputStream(fileInputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                bytesAvailable = buffInputStream.available();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
            buffer = new byte[bufferSize];

            try {
                bytesRead = buffInputStream.read(buffer, 0, bufferSize);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            try {
                while (bytesRead > 0) {
                    try {
                        out.write(buffer, 0, bufferSize);
                        out.flush();

                        // Log.i(TAG, "bytes written: " + bytesRead);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        isSucccessful = false;
                    }
                    bytesAvailable = buffInputStream.available();
                    bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
                    bytesRead = buffInputStream.read(buffer, 0, bufferSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
                isSucccessful = false;
            }
        } else {
            Log.w(TAG, "#uploadObjectByUri out stream is null");
        }

        try {
            int responseCode = connection.getResponseCode();
            Log.v(TAG, "#uploadObjectByUri resCode: " + responseCode);

            if (responseCode == HttpStatus.SC_OK) {
                isSucccessful = true;
            } else {
                isSucccessful = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "#uploadObjectByUri <====Uploading finished====>");

        return isSucccessful;
    }

    /**
     * Get the file path from the Media Content Uri for video, audio or images.
     * <p/>
     * **
     *//*
    public static String getPathForMediaUri(Context context, Uri mediaContentUri) {

        Cursor cur = null;
        String path = null;

        try {
            String[] projection = { MediaColumns.DATA };
            cur = context.getContentResolver().query(mediaContentUri, projection, null, null, null);

            if (cur != null && cur.getCount() != 0) {
                cur.moveToFirst();
                path = cur.getString(cur.getColumnIndexOrThrow(MediaColumns.DATA));
            }

            // Log.v( TAG, "#getRealPathFromURI Path: " + path );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cur != null && !cur.isClosed())
                cur.close();
        }

        return path;
    }*/
    public void setTemporaryHeaders(LinkedHashMap<String, String> tempHeadersList) {
        mTempHeadersList = tempHeadersList;
    }

    ///////////////////////////////upload attachment//////////////////////
//    private static final String NEW_LINE = "\r\n";
//    private static final String TWO_HYPHENS = "--";
//    private static final int BUFFER_SIZE = 4096;
//
//    private boolean handleFileUpload(final String uploadId, final String url, final String method, Uri uri, String contentType) throws IOException {
//
//        final String boundary = getBoundary();
//        final byte[] boundaryBytes = getBoundaryBytes(boundary);
//
//        HttpURLConnection conn = null;
//        OutputStream requestStream = null;
//
//        try {
//            conn = getMultipartHttpURLConnection(url, method, boundary);
//
//            setRequestHeaders(conn, contentType);
//
//            requestStream = conn.getOutputStream();
//            //setRequestParameters(requestStream, requestParameters, boundaryBytes);
//
//            uploadFiles(uploadId, requestStream, uri, boundaryBytes, contentType);
//
//            final byte[] trailer = getTrailerBytes(boundary);
//            requestStream.write(trailer, 0, trailer.length);
//            final int serverResponseCode = conn.getResponseCode();
//            final String serverResponseMessage = conn.getResponseMessage();
//
//            // broadcastCompleted(uploadId, serverResponseCode, serverResponseMessage);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        } catch (Error e) {
//            e.printStackTrace();
//            return false;
//        } finally {
//            closeOutputStream(requestStream);
//            closeConnection(conn);
//        }
//
//        return true;
//    }
//
//    private String getBoundary() {
//        final StringBuilder builder = new StringBuilder();
//
//        builder.append("---------------------------").append(System.currentTimeMillis());
//
//        return builder.toString();
//    }
//
//    private byte[] getBoundaryBytes(final String boundary) throws UnsupportedEncodingException {
//        final StringBuilder builder = new StringBuilder();
//
//        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(NEW_LINE);
//
//        return builder.toString().getBytes("US-ASCII");
//    }
//
//    private HttpURLConnection getMultipartHttpURLConnection(final String url, final String method, final String boundary) throws IOException {
//        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//
//        conn.setDoInput(true);
//        conn.setDoOutput(true);
//        conn.setUseCaches(false);
//        conn.setChunkedStreamingMode(0);
//        conn.setRequestMethod(method);
//        conn.setRequestProperty("Connection", "Keep-Alive");
//        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
//        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
//
//        return conn;
//    }
//
//    private void setRequestHeaders(final HttpURLConnection conn, String contentType) {
//        conn.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
//        conn.setRequestProperty(HttpHeader.ACCEPT, "*/*");
//    }
//
//    private void uploadFiles(final String uploadId, final OutputStream requestStream, Uri uri, final byte[] boundaryBytes, String contentType) throws UnsupportedEncodingException, IOException, FileNotFoundException {
//
//        String path = Utils.getPathForMediaUri(mContext, uri);
//        File file = new File(path);
//
//        final long totalBytes = file.length();
//        long uploadedBytes = 0;
//
//        requestStream.write(boundaryBytes, 0, boundaryBytes.length);
//        byte[] headerBytes = getMultipartHeader(file.getName(), contentType);
//        requestStream.write(headerBytes, 0, headerBytes.length);
//
//        final InputStream stream = getStream(file);
//        byte[] buffer = new byte[BUFFER_SIZE];
//        long bytesRead;
//
//        try {
//            while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0) {
//                requestStream.write(buffer, 0, buffer.length);
//                uploadedBytes += bytesRead;
//                //broadcastProgress(uploadId, uploadedBytes, totalBytes);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeInputStream(stream);
//        }
//
//    }
//
//    public byte[] getMultipartHeader(String fileName, String contentType) throws UnsupportedEncodingException {
//        StringBuilder builder = new StringBuilder();
//
//        builder.append("Content-Disposition: form-data; name=\"")
//                .append("uploaded_file")
//                .append("\"; filename=\"")
//                .append(fileName)
//                .append("\"")
//                .append(NEW_LINE);
//
//        if (contentType != null) {
//            builder.append("Content-Type: ")
//                    .append(contentType)
//                    .append(NEW_LINE);
//        }
//
//        builder.append(NEW_LINE);
//
//        return builder.toString().getBytes("UTF-8");
//    }
//
//    public final InputStream getStream(File file) throws FileNotFoundException {
//        return new FileInputStream(file);
//    }
//
//    private void closeInputStream(final InputStream stream) {
//        if (stream != null) {
//            try {
//                stream.close();
//            } catch (Exception exc) {
//            }
//        }
//    }
//
//    private void closeOutputStream(final OutputStream stream) {
//        if (stream != null) {
//            try {
//                stream.flush();
//                stream.close();
//            } catch (Exception exc) {
//            }
//        }
//    }
//
//    private void closeConnection(final HttpURLConnection connection) {
//        if (connection != null) {
//            try {
//                connection.disconnect();
//            } catch (Exception exc) {
//            }
//        }
//    }
//
//    private byte[] getTrailerBytes(final String boundary) throws UnsupportedEncodingException {
//        final StringBuilder builder = new StringBuilder();
//
//        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(TWO_HYPHENS).append(NEW_LINE);
//
//        return builder.toString().getBytes("US-ASCII");
//    }
    //////////////////////////////////////////////////////////////////
}
