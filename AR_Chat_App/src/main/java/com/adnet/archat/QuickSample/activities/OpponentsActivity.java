package com.adnet.archat.QuickSample.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.adnet.archat.ARChatApp;
import com.adnet.archat.Core.utils.SharedPrefsHelper;
import com.adnet.archat.NativeFunc;
import com.adnet.archat.QuickSample.dialogs.SelectANT_CameraDialog;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.adnet.archat.Core.utils.Toaster;
import com.adnet.archat.R;
import com.adnet.archat.QuickSample.adapters.OpponentsAdapter;
import com.adnet.archat.QuickSample.db.QbUsersDbManager;
import com.adnet.archat.QuickSample.services.CallService;
import com.adnet.archat.Consts;
import com.adnet.archat.QuickSample.utils.PermissionsChecker;
import com.adnet.archat.QuickSample.utils.PushNotificationSender;
import com.adnet.archat.QuickSample.utils.UsersUtils;
import com.adnet.archat.QuickSample.utils.WebRtcSessionManager;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * QuickBlox team
 */
public class OpponentsActivity extends BaseActivity implements SelectANT_CameraDialog.SelectANT_CameraListener {
    private static final String TAG = OpponentsActivity.class.getSimpleName();

    private static final long ON_ITEM_CLICK_DELAY = TimeUnit.SECONDS.toMillis(10);

    private OpponentsAdapter opponentsAdapter;
    private ListView opponentsListView;
    private QBUser currentUser;
    private ArrayList<QBUser> currentOpponentsList;
    private QbUsersDbManager dbManager;
    private boolean isRunForCall;
    private WebRtcSessionManager webRtcSessionManager;

    private PermissionsChecker checker;
    private int selectedPosition = 0;

    private BaseLoaderCallback myLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    NativeFunc.loadLibrary();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public static void start(Context context, boolean isRunForCall) {
        Intent intent = new Intent(context, OpponentsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(Consts.EXTRA_IS_STARTED_FOR_CALL, isRunForCall);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opponents);

        initFields();

        initDefaultActionBar();

        initUi();

        startLoadUsers();

        if (isRunForCall && webRtcSessionManager.getCurrentSession() != null) {
            CallActivity.start(OpponentsActivity.this, true);
        }

        checker = new PermissionsChecker(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUsersList();
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, myLoaderCallback);
        } else {
            myLoaderCallback.onManagerConnected(0);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            isRunForCall = intent.getExtras().getBoolean(Consts.EXTRA_IS_STARTED_FOR_CALL);
            if (isRunForCall && webRtcSessionManager.getCurrentSession() != null) {
                CallActivity.start(OpponentsActivity.this, true);
            }
        }
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_opponents);
    }

    private void startPermissionsActivity(boolean checkOnlyAudio) {
        PermissionsActivity.startActivity(this, checkOnlyAudio, Consts.PERMISSIONS);
    }

    private void initFields() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isRunForCall = extras.getBoolean(Consts.EXTRA_IS_STARTED_FOR_CALL);
        }

        currentUser = sharedPrefsHelper.getQbUser();
        dbManager = QbUsersDbManager.getInstance(getApplicationContext());
        webRtcSessionManager = WebRtcSessionManager.getInstance(getApplicationContext());
    }

    private void startLoadUsers() {
        showProgressDialog(R.string.dlg_loading_opponents);
        requestExecutor.loadUsersByTag(getResources().getString(R.string.users_tag), new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> result, Bundle params) {
                hideProgressDialog();
                dbManager.saveAllUsers(result, true);
                initUsersList();
            }

            @Override
            public void onError(QBResponseException responseException) {
                hideProgressDialog();
                showErrorSnackbar(R.string.loading_users_error, responseException, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startLoadUsers();
                    }
                });
            }
        });
    }

    private void initUi() {
        opponentsListView = (ListView) findViewById(R.id.list_opponents);
    }

    private boolean isCurrentOpponentsListActual(ArrayList<QBUser> actualCurrentOpponentsList) {
        boolean equalActual = actualCurrentOpponentsList.retainAll(currentOpponentsList);
        boolean equalCurrent = currentOpponentsList.retainAll(actualCurrentOpponentsList);
        return !equalActual && !equalCurrent;
    }

    private void initUsersList() {
//      checking whether currentOpponentsList is actual, if yes - return
        if (currentOpponentsList != null) {
            ArrayList<QBUser> actualCurrentOpponentsList = dbManager.getAllUsers();
            actualCurrentOpponentsList.remove(sharedPrefsHelper.getQbUser());
            if (isCurrentOpponentsListActual(actualCurrentOpponentsList)) {
                return;
            }
        }
        proceedInitUsersList();
    }

    private void proceedInitUsersList() {
        currentOpponentsList = dbManager.getAllUsers();
        Log.d(TAG, "proceedInitUsersList currentOpponentsList= " + currentOpponentsList);
        currentOpponentsList.remove(sharedPrefsHelper.getQbUser());
        opponentsAdapter = new OpponentsAdapter(this, currentOpponentsList);
//        opponentsAdapter.setSelectedItemsCountsChangedListener(new OpponentsAdapter.SelectedItemsCountsChangedListener() {
//            @Override
//            public void onCountSelectedItemsChanged(int count) {
//                updateActionBar(count);
//            }
//        });
        opponentsListView.setAdapter(opponentsAdapter);
        opponentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                SelectANT_CameraDialog selectCamDialog = new SelectANT_CameraDialog(OpponentsActivity.this,
                        SharedPrefsHelper.getInstance().getQbUser().getLogin(),
                        opponentsAdapter.getItem(position).getLogin(),
                        OpponentsActivity.this);
                selectCamDialog.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (opponentsAdapter != null && !opponentsAdapter.getSelectedItems().isEmpty()) {
            getMenuInflater().inflate(R.menu.activity_selected_opponents, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_opponents, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.update_opponents_list:
                startLoadUsers();
                return true;

            case R.id.settings:
                showSettings();
                return true;

            case R.id.log_out:
                logOut();
                return true;

            case R.id.start_video_call:
//                if (isLoggedInChat()) {
//                    startCall(true);
//                }
//                if (checker.lacksPermissions(Consts.PERMISSIONS)) {
//                    startPermissionsActivity(false);
//                }
                return true;

            case R.id.start_audio_call:
//                if (isLoggedInChat()) {
//                    startCall(false);
//                }
//                if (checker.lacksPermissions(Consts.PERMISSIONS[1])) {
//                    startPermissionsActivity(true);
//                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isLoggedInChat() {
        if (!QBChatService.getInstance().isLoggedIn()) {
            Toaster.shortToast(R.string.dlg_signal_error);
            tryReLoginToChat();
            return false;
        }
        return true;
    }

    private void tryReLoginToChat() {
        if (sharedPrefsHelper.hasQbUser()) {
            QBUser qbUser = sharedPrefsHelper.getQbUser();
            CallService.start(this, qbUser);
        }
    }

    private void showSettings() {
        SettingsActivity.start(this);
    }

    private void startCall(int positioin) {
        if (opponentsAdapter.getSelectedItems().size() > Consts.MAX_OPPONENTS_COUNT) {
            Toaster.longToast(String.format(getString(R.string.error_max_opponents_count),
                    Consts.MAX_OPPONENTS_COUNT));
            return;
        }

        Log.d(TAG, "startCall()");
        ArrayList<Integer> opponentsList = new ArrayList<>();
        opponentsList.add(opponentsAdapter.getItem(positioin).getId());
        final QBRTCTypes.QBConferenceType conferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO;

        QBRTCClient qbrtcClient = QBRTCClient.getInstance(getApplicationContext());

        QBRTCSession newQbRtcSession = qbrtcClient.createNewSessionWithOpponents(opponentsList, conferenceType);

        WebRtcSessionManager.getInstance(this).setCurrentSession(newQbRtcSession);

        PushNotificationSender.sendPushMessage(opponentsList, currentUser.getFullName());

        CallActivity.start(OpponentsActivity.this, false);
        Log.d(TAG, "conferenceType = " + conferenceType);

    }

    private void initActionBarWithSelectedUsers(int countSelectedUsers) {
        setActionBarTitle(String.format(getString(
                countSelectedUsers > 1
                        ? R.string.tile_many_users_selected
                        : R.string.title_one_user_selected),
                countSelectedUsers));
    }

    private void updateActionBar(int countSelectedUsers) {
        if (countSelectedUsers < 1) {
            initDefaultActionBar();
        } else {
            removeActionbarSubTitle();
            initActionBarWithSelectedUsers(countSelectedUsers);
        }

        invalidateOptionsMenu();
    }

    private void logOut() {
        unsubscribeFromPushes();
        startLogoutCommand();
        removeAllUserData();
        startLoginActivity();
    }

    private void startLogoutCommand() {
        CallService.logout(this);
    }

    private void unsubscribeFromPushes() {
        if (googlePlayServicesHelper.checkPlayServicesAvailable(this)) {
            Log.d(TAG, "unsubscribeFromPushes()");
            googlePlayServicesHelper.unregisterFromGcm(Consts.GCM_SENDER_ID);
        }
    }

    private void removeAllUserData() {
        UsersUtils.removeUserData(getApplicationContext());
//        requestExecutor.deleteCurrentUser(currentUser.getId(), new QBEntityCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid, Bundle bundle) {
//                Log.d(TAG, "Current user was deleted from QB");
//            }
//
//            @Override
//            public void onError(QBResponseException e) {
//                Log.e(TAG, "Current user wasn't deleted from QB " + e);
//            }
//        });
    }

    private void startLoginActivity() {
        LoginActivity.start(this);
        finish();
    }

    @Override
    public void SelectedAnnotationCamera(String userCameraName) {
        ARChatApp.ant_camera_Name = userCameraName;
        if (isLoggedInChat()) {
            startCall(selectedPosition);
        }
        if (checker.lacksPermissions(Consts.PERMISSIONS)) {
            startPermissionsActivity(false);
        }
    }
}