package com.adnet.archat.QuickSample.services.gcm;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.adnet.archat.Core.gcm.CoreGcmPushListenerService;
import com.adnet.archat.Core.utils.SharedPrefsHelper;
import com.adnet.archat.Core.utils.constant.GcmConsts;
import com.adnet.archat.QuickSample.services.CallService;
import com.quickblox.users.model.QBUser;

/**
 * Created by tereha on 13.05.16.
 */
public class GcmPushListenerService extends GcmListenerService {
    private static final String TAG = CoreGcmPushListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString(GcmConsts.EXTRA_GCM_MESSAGE);
        Log.v(TAG, "From: " + from);
        Log.v(TAG, "Message: " + message);

        SharedPrefsHelper sharedPrefsHelper = SharedPrefsHelper.getInstance();
        if (sharedPrefsHelper.hasQbUser()) {
            Log.d(TAG, "ARChatApp have logined user");
            QBUser qbUser = sharedPrefsHelper.getQbUser();
            startLoginService(qbUser);
        }
    }

    private void startLoginService(QBUser qbUser){
        CallService.start(this, qbUser);
    }
}