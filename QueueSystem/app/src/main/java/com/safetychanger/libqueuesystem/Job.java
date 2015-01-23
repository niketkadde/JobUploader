package com.safetychanger.libqueuesystem;

import java.util.LinkedHashMap;

/**
 * Data structure for Queue.
 *
 * @author niket
 */
public class Job {

    /**
     * Queue of messages to be stored.
     */
    public static final String TABLE_JOB_QUEUE = "job_queue";

    public static final String COL_ID = "_id";

    /**
     * Data to be uploaded to the queue
     */
    public static final String COL_DATA = "data";

    /**
     * URL where the data is to be posted
     */
    public static final String COL_URL = "url";

    /**
     * Headers to be sent to the collector service. JSON is stored.
     */
    public static final String COL_HEADERS = "headers";

    /**
     * URL where the data is to be posted
     */
    public static final String COL_STATUS = "status";

    /**
     * Specifies type of the data
     */
    public static final String COL_DATA_TYPE = "data_type";

    public static final String COL_ERROR_CODE = "error_code";
    public static final String COL_RETRY_COUNT = "retry_count";
    public static final String COL_JOB_TYPE = "job_type";
    public static final String COL_EXTRA_PARAM_ONE = "extra_parameter_one";

    public static final String CREATE_TABLE_QUEUE = "CREATE TABLE IF NOT EXISTS " + TABLE_JOB_QUEUE + " (" +
            COL_ID + " TEXT NOT NULL, " +
            COL_DATA + " BLOB NOT NULL, " +
            COL_URL + " TEXT NOT NULL, " +
            COL_HEADERS + " BLOB NOT NULL, " +
            COL_DATA_TYPE + " TEXT NOT NULL, " +
            COL_STATUS + " INTEGER NOT NULL," +
            COL_ERROR_CODE + " TEXT," +
            COL_RETRY_COUNT + " INTEGER," +
            COL_JOB_TYPE + " TEXT, " +
            COL_EXTRA_PARAM_ONE + " TEXT); ";

    public static final String DROP_QUEUE_TABLE = "DROP TABLE IF EXISTS " + TABLE_JOB_QUEUE;

    /**
     * Default constructor
     */
    public Job() {
        super();
    }

    /**
     *
     */
    public Job(String id, String data, String url, LinkedHashMap<String, String> headers, int status, String dataType, String errorCode, int retryCount, String jobType, String extraParameterOne) {
        super();
        this.mId = id;
        mData = data;
        mUrl = url;
        mHeadersList = headers;
        mStatus = status;
        mDataTpe = dataType;
        mErrorCode = errorCode;
        mRetryCount = retryCount;
        mJobType = jobType;
        mExtraParameterOne = extraParameterOne;
    }

    public String mId;
    public String mData;
    public String mUrl;
    public LinkedHashMap<String, String> mHeadersList;
    public String mDataTpe;
    public int mStatus;
    public String mErrorCode;
    public int mRetryCount;
    public String mJobType;
    public String mExtraParameterOne;

}
