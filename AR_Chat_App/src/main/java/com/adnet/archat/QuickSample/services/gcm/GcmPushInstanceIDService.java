package com.adnet.archat.QuickSample.services.gcm;

import com.adnet.archat.Core.gcm.CoreGcmPushInstanceIDService;
import com.adnet.archat.Consts;

public class GcmPushInstanceIDService extends CoreGcmPushInstanceIDService {
    @Override
    protected String getSenderId() {
        return Consts.GCM_SENDER_ID;
    }
}
