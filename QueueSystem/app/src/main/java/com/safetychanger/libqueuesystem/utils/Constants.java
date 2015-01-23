package com.safetychanger.libqueuesystem.utils;

import nl.changer.GlobalConstants;

/**
 * Anything that is DATA_* is the key for a particular values stored in SharedPreferences.
 * <p/>
 *
 * @author niket
 */
public class Constants extends GlobalConstants {

    /**
     * Flag to enable the app behave in debug mode. This can be used to ease the testing, debugging
     * of the app. In the production release this flag should be set to false;
     */
    public static boolean IS_DEV = false;

    public static String DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static String URL_GOOGLE_PLAY_STORE = "https://play.google.com/store/apps/details?id=";
}
