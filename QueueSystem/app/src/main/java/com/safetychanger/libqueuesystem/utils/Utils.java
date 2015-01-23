package com.safetychanger.libqueuesystem.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nl.changer.android.opensource.DateUtils;

public class Utils extends nl.changer.android.opensource.Utils {

    /**
     * @param ctx
     * @deprecated
     */
    public Utils(Context ctx) {
        super(ctx);
    }

    /**
     * Compare two dates.
     *
     * @param firstDate
     * @param secondDate
     * @return 0 if the dates are equal <br/>
     * 1 if firstDate is after secondDate <br/>
     * 2 if firstDate is before secondDate <br/>
     * -1 if any error occurred while comparing two dates.
     */
    public static int compareDates(Calendar firstDate, Calendar secondDate) {

        try {

            if (firstDate != null && secondDate != null) {
                if (firstDate.after(secondDate)) {
                    return 1;
                } else if (firstDate.before(secondDate)) {
                    return 2;
                } else {
                    return 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * Formats and returns given ISO formatted date in the required format, if any exception occurs
     * it returns date in default format i.e. dd MMM, yyyy.
     *
     * @param isoDate
     * @param format
     * @return
     */
    public static String formatDate(String isoDate, String format) {

        if (TextUtils.isEmpty(isoDate)) {
            return isoDate;
        }

        if (TextUtils.isEmpty(format)) {
            format = "dd MMM, yyyy";
        }

        String formattedDate = null;

        try {

            Date date = DateUtils.parseDate(isoDate);
            formattedDate = new SimpleDateFormat(format).format(date);

        } catch (Exception e) {
            e.printStackTrace();
            formattedDate = isoDate;
        }

        return formattedDate;
    }

    /**
     * Converts UTC time to device local time.
     * If any error occurs while conversion this method will return passed parameter value.
     *
     * @param utcTimeIsoFormat Pass UTC time in ISO format "yyyy-MM-dd'T'HH:mm:ss"
     * @return converted local time in  ISO format "yyyy-MM-dd'T'HH:mm:ss"
     */
    public static String getTimeUtcToLocal(String utcTimeIsoFormat) {
        try {
            String isoFormat = "yyyy-MM-dd'T'HH:mm:ss";
            String newDate = utcTimeIsoFormat.substring(0, 19).toString();
            SimpleDateFormat formatter = new SimpleDateFormat(isoFormat);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date value = null;
            try {
                value = formatter.parse(newDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            SimpleDateFormat dateFormatter = new SimpleDateFormat(isoFormat);
            dateFormatter.setTimeZone(TimeZone.getDefault());
            String dt = dateFormatter.format(value);

            return dt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return utcTimeIsoFormat;
    }

    /**
     * Gets system current date time and formats in ISO date format.
     *
     * @return returns system current date/time in ISO format i.e. yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String getSystemCurrentDateTime() {
        String currentDate = null;
        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_ISO);
        Calendar calendar = Calendar.getInstance();
        currentDate = dateFormat.format(calendar.getTime());
        return currentDate;
    }

    /**
     * Formats given calendar date in ISO format i.e. yyyy-MM-dd'T'HH:mm:ssZ
     *
     * @param calendar
     * @return
     */
    public static String getIsoFormattedDate(Calendar calendar) {
        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_ISO);
        return dateFormat.format(calendar.getTime());
    }

    /**
     * Shows keypad.
     *
     * @param context
     */
    public static void showKeyPad(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Hides keypad.
     *
     * @param context
     * @param editText
     */
    public static void hideKeyPad(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ", new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns application's current version.
     *
     * @param activity
     * @return String
     */
    public static String getAppCurrentVersionName(Activity activity) {
        PackageInfo packageInfo;
        String versionName = "1.0.0"; // default
        try {
            packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            versionName = packageInfo.versionName;
            int vCode = packageInfo.versionCode;
        } catch (Exception e) {
            DebugLog.e(e.toString());
        } catch (Error e) {
            DebugLog.e(e.toString());
        }
        return versionName;
    }

    /**
     * Opens Google Play Store app, and redirects to running app on play store using package name.
     *
     * @param activity
     */
    public static void openPlayStoreApp(Activity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_GOOGLE_PLAY_STORE + activity.getApplicationContext().getPackageName()));
        activity.startActivity(browserIntent);
    }

}