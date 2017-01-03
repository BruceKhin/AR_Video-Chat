package com.adnet.archat;

import android.Manifest;

/**
 * QuickBlox team
 */
public interface Consts {

    String APP_ID = "46817";
    String AUTH_KEY = "Lrukhbx7huAP-Yc";
    String AUTH_SECRET = "f4zcPa4tjkQAvsm";
    String ACCOUNT_KEY = "QWwDxxdoUo7xBckxyqxb";

    int ANT_MY_CAMERA = 1;
    int ANT_OPPONENT_CAMERA = 2;

    // In GCM, the Sender ID is a project ID that you acquire from the API console
    String GCM_SENDER_ID = "760200355915";

    String DEFAULT_USER_PASSWORD = "x6Bt0VDy5";

    String VERSION_NUMBER = "1.0";

    int CALL_ACTIVITY_CLOSE = 1000;

    int ERR_LOGIN_ALREADY_TAKEN_HTTP_STATUS = 422;
    int ERR_MSG_DELETING_HTTP_STATUS = 401;

    //CALL ACTIVITY CLOSE REASONS
    int CALL_ACTIVITY_CLOSE_WIFI_DISABLED = 1001;
    String WIFI_DISABLED = "wifi_disabled";

    String OPPONENTS = "opponents";
    String CONFERENCE_TYPE = "conference_type";
    String EXTRA_TAG = "currentRoomName";
    int MAX_OPPONENTS_COUNT = 6;

    String PREF_CURREN_ROOM_NAME = "current_room_name";
    String PREF_CURRENT_TOKEN = "current_token";
    String PREF_TOKEN_EXPIRATION_DATE = "token_expiration_date";

    String EXTRA_QB_USER = "qb_user";

    String EXTRA_USER_ID = "user_id";
    String EXTRA_USER_LOGIN = "user_login";
    String EXTRA_USER_PASSWORD = "user_password";
    String EXTRA_PENDING_INTENT = "pending_Intent";

    String EXTRA_CONTEXT = "context";
    String EXTRA_OPPONENTS_LIST = "opponents_list";
    String EXTRA_CONFERENCE_TYPE = "conference_type";
    String EXTRA_IS_INCOMING_CALL = "conversation_reason";

    String EXTRA_LOGIN_RESULT = "login_result";
    String EXTRA_LOGIN_ERROR_MESSAGE = "login_error_message";
    int EXTRA_LOGIN_RESULT_CODE = 1002;

    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    String EXTRA_COMMAND_TO_SERVICE = "command_for_service";
    int COMMAND_NOT_FOUND = 0;
    int COMMAND_LOGIN = 1;
    int COMMAND_LOGOUT = 2;
    String EXTRA_IS_STARTED_FOR_CALL = "isRunForCall";
    String ALREADY_LOGGED_IN = "You have already logged in chat";

    String IMG_GRAY = "img_gray";
    String ARRAY_TRACK_BOX = "arrayTrackBox";
    String FREE_DRAW_POINT = "freeDrawPoint";
    String X = "x";
    String Y = "y";
    String WIDTH = "width";
    String HEIGHT = "height";
    String IMG_WIDTH = "img_width";
    String IMG_HEIGHT = "img_height";
    String POINT_ARRAY = "ptArray";
    String COLOR = "color";
    String FEATURE_INDEX = "featureCNT";
    String USER_NAME = "username";
    String USER_PASSWORD = "userpassword";
    String USER_LOGGEDIN= "loggedin";

    enum StartConversationReason {
        INCOME_CALL_FOR_ACCEPTION,
        OUTCOME_CALL_MADE
    }
}
