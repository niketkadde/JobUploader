package com.safetychanger.libqueuesystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Class handles all the Job table related operations.
 *
 * @author niket
 */
public class JobDbManager {
    private DbHelper mDbHelper;
    private Context mContext;

    /**
     * Default Constructor
     *
     * @param context
     */
    public JobDbManager(Context context) {
        mContext = context;
        mDbHelper = DbHelper.getInstance(context);
    }

    public void addJob(final Handler handler, final Job job) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                addJobSync(handler, job);
            }
        }).start();
    }

    public void addJob(Job job) {
        addJobSync(null, job);
    }

    private void addJobSync(Handler handler, Job job) {
        try {

            //if (getJobById(job.mId) == null) {
            mDbHelper.beginTransaction();
            ContentValues contentValues = new ContentValues();
            contentValues.put(Job.COL_ID, job.mId);
            contentValues.put(Job.COL_DATA, job.mData);
            contentValues.put(Job.COL_URL, job.mUrl);
            Gson gson = new Gson();
            String headers = gson.toJson(job.mHeadersList);
            contentValues.put(Job.COL_HEADERS, headers);
            contentValues.put(Job.COL_DATA_TYPE, job.mDataTpe);
            contentValues.put(Job.COL_STATUS, job.mStatus);
            contentValues.put(Job.COL_ERROR_CODE, job.mErrorCode);
            contentValues.put(Job.COL_RETRY_COUNT, job.mRetryCount);
            contentValues.put(Job.COL_JOB_TYPE, job.mJobType);
            contentValues.put(Job.COL_EXTRA_PARAM_ONE, job.mExtraParameterOne);

            mDbHelper.insert(Job.TABLE_JOB_QUEUE, contentValues);
            mDbHelper.endTransaction();
            //}
        } catch (Exception e) {
            e.printStackTrace();
            mDbHelper.endTransaction();
        }
    }

    public Job getJobById(String jobId) {
        Job job = null;
        try {
            Cursor cursor = mDbHelper.select(Job.TABLE_JOB_QUEUE, null, Job.COL_ID + "=?", new String[]
                    {jobId}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                job = new Job();
                job.mId = cursor.getString(cursor.getColumnIndex(Job.COL_ID));
                job.mData = cursor.getString(cursor.getColumnIndex(Job.COL_DATA));
                job.mUrl = cursor.getString(cursor.getColumnIndex(Job.COL_URL));
                job.mHeadersList = jsonToMap(cursor.getString(cursor.getColumnIndex(Job.COL_HEADERS)));
                job.mDataTpe = cursor.getString(cursor.getColumnIndex(Job.COL_DATA_TYPE));
                job.mStatus = cursor.getInt(cursor.getColumnIndex(Job.COL_STATUS));
                job.mErrorCode = cursor.getString(cursor.getColumnIndex(Job.COL_ERROR_CODE));
                job.mRetryCount = cursor.getInt(cursor.getColumnIndex(Job.COL_RETRY_COUNT));
                job.mJobType = cursor.getString(cursor.getColumnIndex(Job.COL_JOB_TYPE));
                job.mExtraParameterOne = cursor.getString(cursor.getColumnIndex(Job.COL_EXTRA_PARAM_ONE));

                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return job;
    }

    public ArrayList<Job> getAllJobBy(int status) {
        ArrayList<Job> jobList = null;
        try {
            Cursor cursor = null;
            cursor = mDbHelper.select(Job.TABLE_JOB_QUEUE, null, Job.COL_STATUS + "=?", new String[]
                    {String.valueOf(status)}, null, null, null);

            if (cursor.moveToFirst()) {
                jobList = new ArrayList<Job>();
                while (!cursor.isAfterLast()) {
                    Job job = new Job();
                    job.mId = cursor.getString(cursor.getColumnIndex(Job.COL_ID));
                    job.mData = cursor.getString(cursor.getColumnIndex(Job.COL_DATA));
                    job.mUrl = cursor.getString(cursor.getColumnIndex(Job.COL_URL));
                    job.mHeadersList = jsonToMap(cursor.getString(cursor.getColumnIndex(Job.COL_HEADERS)));
                    job.mDataTpe = cursor.getString(cursor.getColumnIndex(Job.COL_DATA_TYPE));
                    job.mStatus = cursor.getInt(cursor.getColumnIndex(Job.COL_STATUS));
                    job.mErrorCode = cursor.getString(cursor.getColumnIndex(Job.COL_ERROR_CODE));
                    job.mRetryCount = cursor.getInt(cursor.getColumnIndex(Job.COL_RETRY_COUNT));
                    job.mJobType = cursor.getString(cursor.getColumnIndex(Job.COL_JOB_TYPE));
                    job.mExtraParameterOne = cursor.getString(cursor.getColumnIndex(Job.COL_EXTRA_PARAM_ONE));

                    jobList.add(job);
                    cursor.moveToNext();
                }
            }

            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jobList;
    }

    /**
     * Empties the database Job table
     */
    public void deleteAllJobs() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mDbHelper.beginTransaction();
                    mDbHelper.delete(Job.TABLE_JOB_QUEUE, null, null);
                    mDbHelper.endTransaction();
                } catch (Exception e) {
                    e.printStackTrace();
                    mDbHelper.endTransaction();
                }
            }
        }).start();
    }

    public void updateJobStatus(String jobId, String dataType, int status) {
        try {
            mDbHelper.beginTransaction();
            ContentValues contentValues = new ContentValues();
            contentValues.put(Job.COL_STATUS, status);
            mDbHelper.update(Job.TABLE_JOB_QUEUE, contentValues, Job.COL_ID + "=? AND " + Job.COL_DATA_TYPE + "=?", new String[]{jobId, dataType});
            mDbHelper.endTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            mDbHelper.endTransaction();
        }
    }

    private LinkedHashMap<String, String> jsonToMap(String jsonString) throws JSONException {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        try {
            JSONObject jObject = new JSONObject(jsonString);
            Iterator<?> keys = jObject.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = jObject.getString(key);
                map.put(key, value);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Deletes job from DB by id.
     *
     * @param jobId
     */
    public void deleteJobById(final String jobId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mDbHelper.beginTransaction();
                    mDbHelper.delete(Job.TABLE_JOB_QUEUE, Job.COL_ID + "=?", new String[]{jobId});
                    mDbHelper.endTransaction();
                } catch (Exception e) {
                    e.printStackTrace();
                    mDbHelper.endTransaction();
                }
            }
        }).start();
    }
}