package com.adnet.archat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.adnet.archat.Core.CoreApp;
import com.adnet.archat.QuickSample.util.QBResRequestExecutor;
import com.firebase.client.Firebase;

public class ARChatApp extends CoreApp {
    private static ARChatApp instance;
    private QBResRequestExecutor qbResRequestExecutor;
    public static Firebase mDatabase;
    public static boolean isReceived = false;
    public static String databaseID = "";
    public static int COMPRESS_RATIO = 8;
    public static SharedPreferences appPrefs;
    public static String ant_camera_Name;

    public static ARChatApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();
    }

    private void initApplication(){
        instance = this;
        Firebase.setAndroidContext(this);
        appPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDatabase = new Firebase("https://ar-chat-cc16f.firebaseio.com/");
        super.initCredentials(Consts.APP_ID, Consts.AUTH_KEY, Consts.AUTH_SECRET, Consts.ACCOUNT_KEY);

    }

    public synchronized QBResRequestExecutor getQbResRequestExecutor() {
        return qbResRequestExecutor == null
                ? qbResRequestExecutor = new QBResRequestExecutor()
                : qbResRequestExecutor;
    }
}
