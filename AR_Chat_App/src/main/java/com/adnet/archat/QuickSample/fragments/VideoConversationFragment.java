package com.adnet.archat.QuickSample.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.adnet.archat.ARChatApp;
import com.adnet.archat.Core.utils.SharedPrefsHelper;
import com.adnet.archat.Item.FeatureItem;
import com.adnet.archat.NativeFunc;
import com.adnet.archat.QuickSample.activities.CallActivity;
import com.adnet.archat.QuickSample.adapters.OpponentsFromCallAdapter;
import com.adnet.archat.QuickSample.view.OpenCV_RTCRemoteVideoView;
import com.adnet.archat.QuickSample.view.OpenCV_RTCVideoView;
import com.adnet.archat.R;
import com.adnet.archat.UI.DrawingView;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBMediaStreamManager;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.exception.QBRTCException;
import com.quickblox.videochat.webrtc.view.QBRTCSurfaceView;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * QuickBlox team
 */
public class VideoConversationFragment extends BaseConversationFragment implements Serializable, QBRTCClientVideoTracksCallbacks,
        QBRTCSessionConnectionCallbacks, CallActivity.QBRTCSessionUserCallback, DrawingView.DrawingListener, OpenCV_RTCVideoView.bitmapListner, OpenCV_RTCRemoteVideoView.bitmapListner {

    private static final int DEFAULT_ROWS_COUNT = 2;
    private static final int DEFAULT_COLS_COUNT = 3;
    private static final long TOGGLE_CAMERA_DELAY = 1000;
    private static final long LOCAL_TRACk_INITIALIZE_DELAY = 500;
    private static final int RECYCLE_VIEW_PADDING = 2;
    private static final long UPDATING_USERS_DELAY = 2000;
    private static final long FULL_SCREEN_CLICK_DELAY = 1000;

    private String TAG = VideoConversationFragment.class.getSimpleName();

    private ToggleButton cameraToggle;
    private View view;
    private boolean isVideoCall = false;
    private LinearLayout actionVideoButtonsLayout;
    private OpenCV_RTCRemoteVideoView opponentVideoView;
    private OpenCV_RTCVideoView localVideoView;
    private CameraState cameraState = CameraState.NONE;
    private SparseArray<OpponentsFromCallAdapter.ViewHolder> opponentViewHolders;
    private List<OpponentsFromCallAdapter.ViewHolder> viewHolders;
    private boolean isPeerToPeerCall;
    private QBRTCVideoTrack localVideoTrack;
    private QBRTCVideoTrack remoteVideoTrack;
    private TextView connectionStatusLocal;

    private Map<Integer, QBRTCVideoTrack> videoTrackMap;
    private OpponentsFromCallAdapter opponentsAdapter;
    private LocalViewOnClickListener localViewOnClickListener;
    private boolean isRemoteShown;
    private boolean headsetPlugged;

    private int amountOpponents;
    private int userIDFullScreen;
    private List<QBUser> allOpponents;
    private boolean connectionEstablished;
    private boolean previousDeviceEarPiece;
    private boolean allCallbacksInit;
    private boolean isCurrentCameraFront = true;
    private boolean isLocalVideoFullScreen;

    private MenuItem switchCameraMenuItem;
    private RelativeLayout rlAnnotationCtrl;

    private DrawingView drawingView;
    private ImageView ivDrawBlack, ivDrawRed, ivDrawBlue, ivDrawGreen, ivCloseAnnotation;
            //The buttons those are color of the mark line, and close button of the annotation.
    private Button btnReset, btnUndo;// the buttons of undo and reset.

    ImageView opencvMarkDrawnImg;//the imageview of the marks those can be changed by video frame.

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = super.onCreateView(inflater, container, savedInstanceState);
        initVideoTrackSListener();
        return view;
    }

    @Override
    protected void configureOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.grey_transparent_50));
        allOpponentsTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        ringingTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
    }

    @Override
    protected void configureActionBar() {
        actionBar = ((AppCompatActivity) getActivity()).getDelegate().getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    protected void configureToolbar() {
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.black_transparent_50));
        toolbar.setTitleTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        toolbar.setSubtitleTextColor(ContextCompat.getColor(getActivity(), R.color.white));
    }

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_video_conversation;
    }

    @Override
    protected void initFields() {
        super.initFields();
        localViewOnClickListener = new LocalViewOnClickListener();
        amountOpponents = opponents.size();
        allOpponents = Collections.synchronizedList(new ArrayList<QBUser>(opponents.size()));
        allOpponents.addAll(opponents);

        timerChronometer = (Chronometer) getActivity().findViewById(R.id.timer_chronometer_action_bar);

        isPeerToPeerCall = opponents.size() == 1;
        isVideoCall = (QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO.equals(currentSession.getConferenceType()));
        NativeFunc.ResetCMT();//when the call is started, reset all marks.
    }

    public void setDuringCallActionBar() {
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(currentUser.getFullName());
        if (isPeerToPeerCall) {
            actionBar.setSubtitle(getString(R.string.opponent, opponents.get(0).getFullName()));
        } else {
            actionBar.setSubtitle(getString(R.string.opponents, amountOpponents));
        }

        actionButtonsEnabled(true);
    }

    private void initVideoTrackSListener() {
        if (currentSession != null) {
            currentSession.addVideoTrackCallbacksListener(this);
        }
    }

    private void removeVideoTrackSListener() {
        if (currentSession != null) {
            currentSession.removeVideoTrackCallbacksListener(this);
        }
    }

    @Override
    protected void actionButtonsEnabled(boolean inability) {
        super.actionButtonsEnabled(inability);
        cameraToggle.setEnabled(inability);
        // inactivate toggle buttons
        cameraToggle.setActivated(inability);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!allCallbacksInit) {
            conversationFragmentCallbackListener.addTCClientConnectionCallback(this);
            conversationFragmentCallbackListener.addRTCSessionUserCallback(this);
            allCallbacksInit = true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected void initViews(View view) {
        super.initViews(view);

        opponentViewHolders = new SparseArray<>(opponents.size());

        localVideoView = (OpenCV_RTCVideoView) view.findViewById(R.id.local_video_view);
        connectionStatusLocal = (TextView) view.findViewById(R.id.connectionStatusLocal);

        cameraToggle = (ToggleButton) view.findViewById(R.id.toggle_camera);
        cameraToggle.setVisibility(View.VISIBLE);

        actionVideoButtonsLayout = (LinearLayout) view.findViewById(R.id.element_set_video_buttons);

        actionButtonsEnabled(false);

        rlAnnotationCtrl = (RelativeLayout)view.findViewById(R.id.rlAnnotationCtrl);
        ivDrawBlack = (ImageView)view.findViewById(R.id.ivDrawBlack);
        ivDrawRed = (ImageView)view.findViewById(R.id.ivDrawRed);
        ivDrawBlue = (ImageView)view.findViewById(R.id.ivDrawBlue);
        ivDrawGreen = (ImageView)view.findViewById(R.id.ivDrawGreen);
        btnReset = (Button)view.findViewById(R.id.btnReset);
        btnUndo = (Button)view.findViewById(R.id.btnUndo);
        ivCloseAnnotation = (ImageView)view.findViewById(R.id.ivCloseAnnotation);
        rlAnnotationCtrl.setVisibility(View.INVISIBLE);
        drawingView = (DrawingView)view.findViewById(R.id.drawingView);
        drawingView.initVariable(this);
        opencvMarkDrawnImg = (ImageView)view.findViewById(R.id.opencvImg);


        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Read all marks, and remove last item of the marks.
                ((CallActivity)getActivity()).showProgressDialog("Removing Annotation..");
                ARChatApp.mDatabase.child(ARChatApp.databaseID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ((CallActivity)getActivity()).hideProgressDialog();
                        if(dataSnapshot.getChildrenCount() == 0)//if no mark is drawn, it will be returned.
                            return;
                        ARChatApp.mDatabase.child(ARChatApp.databaseID).child(String.valueOf(dataSnapshot.getChildrenCount() - 1)).removeValue();//remove last item.
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
//                NativeFunc.UndoCMT();
            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARChatApp.mDatabase.child(ARChatApp.databaseID).removeValue();//remove all marks from the server.
                //When remove 3 marks, onChildRemoved funtion in CallActivity will be called 3 times automatically. So undoCMT function will be called 3 times.
            }
        });
        //Set the color of marks.
        ivDrawGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setDrawingColor(Color.GREEN);
            }
        });
        ivDrawBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setDrawingColor(Color.BLUE);
            }
        });
        ivDrawRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setDrawingColor(Color.RED);
            }
        });
        ivDrawBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setDrawingColor(Color.BLACK);
            }
        });
        ivCloseAnnotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionBar.show();
                actionVideoButtonsLayout.setVisibility(View.VISIBLE);
                rlAnnotationCtrl.setVisibility(View.INVISIBLE);
                //End the annotation work. and only show mark and video.
            }
        });

    }

    private void initCorrectSizeForLocalView() {
        ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
        DisplayMetrics displaymetrics = getResources().getDisplayMetrics();

        int screenWidthPx = displaymetrics.widthPixels;
        Log.d(TAG, "screenWidthPx " + screenWidthPx);
        params.width = (int) (screenWidthPx * 0.3);
        params.height = (params.width / 2) * 3;
        localVideoView.setLayoutParams(params);
    }

    private Map<Integer, QBRTCVideoTrack> getVideoTrackMap() {
        if (videoTrackMap == null) {
            videoTrackMap = new HashMap<>();
        }
        return videoTrackMap;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If user changed camera state few times and last state was CameraState.ENABLED_FROM_USER
        // than we turn on cam, else we nothing change
        if (cameraState != CameraState.DISABLED_FROM_USER) {
            toggleCamera(true);
        }
    }

    @Override
    public void onPause() {
        // If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
        // than we turn off cam
        if (cameraState != CameraState.DISABLED_FROM_USER) {
            toggleCamera(false);
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (connectionEstablished) {
            conversationFragmentCallbackListener.removeRTCClientConnectionCallback(this);
            conversationFragmentCallbackListener.removeRTCSessionUserCallback(this);
            allCallbacksInit = false;
        } else {
            Log.d(TAG, "We are in dialing process yet!");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        removeVideoTrackSListener();
    }

    protected void initButtonsListener() {
        super.initButtonsListener();

        cameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cameraState = isChecked ? CameraState.ENABLED_FROM_USER : CameraState.DISABLED_FROM_USER;
                toggleCamera(isChecked);
            }
        });
    }

    private void switchCamera(final MenuItem item) {//Switch the camera of my video.
        if (currentSession == null || cameraState == CameraState.DISABLED_FROM_USER) {
            return;
        }
        final QBMediaStreamManager mediaStreamManager = currentSession.getMediaStreamManager();
        if (mediaStreamManager == null) {
            return;
        }
//      disable cameraToggle while processing switchCamera
        cameraToggle.setEnabled(false);

        mediaStreamManager.switchCameraInput(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                Log.d(TAG, "camera switched, bool = " + b);
                isCurrentCameraFront = b;
                updateSwitchCameraIcon(item);
                toggleCameraInternal();
            }

            @Override
            public void onCameraSwitchError(String s) {
                Log.d(TAG, "camera switch error " + s);
                cameraToggle.setEnabled(true);
            }
        });
    }

    private void updateSwitchCameraIcon(final MenuItem item) {
        if (isCurrentCameraFront) {
            Log.d(TAG, "CameraFront now!");
            item.setIcon(R.drawable.ic_camera_front);
        } else {
            Log.d(TAG, "CameraRear now!");
            item.setIcon(R.drawable.ic_camera_rear);
        }
    }

    private void toggleCameraInternal() {
        Log.d(TAG, "Camera was switched!");
        updateVideoView(localVideoView, isCurrentCameraFront);
        toggleCamera(true);
    }

    private void toggleCamera(boolean isNeedEnableCam) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            conversationFragmentCallbackListener.onSetVideoEnabled(isNeedEnableCam);
        }
        if (connectionEstablished && !cameraToggle.isEnabled()) {
            cameraToggle.setEnabled(true);
        }
    }

    ////////////////////////////  callbacks from QBRTCClientVideoTracksCallbacks ///////////////////
    @Override
    public void onLocalVideoTrackReceive(QBRTCSession qbrtcSession, final QBRTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive() run");
        //When my local video is receved, this function will be called one time.
        
        localVideoTrack = videoTrack;
        isLocalVideoFullScreen = true;
        switchCamera(switchCameraMenuItem);

        if (isPeerToPeerCall) {
            Log.d(TAG, "onLocalVideoTrackReceive init localView");
            localVideoView.setOnClickListener(localViewOnClickListener);
            localVideoView.initListener(this);

            if (localVideoTrack != null) {
                fillVideoView(localVideoView, localVideoTrack, false);
            }
        }
        //in other case localVideoView hasn't been inflated yet. Will set track while OnBindLastViewHolder
    }

    @Override
    public void onRemoteVideoTrackReceive(QBRTCSession session, final QBRTCVideoTrack videoTrack, final Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);

        if(ARChatApp.ant_camera_Name.equals(SharedPrefsHelper.getInstance().getQbUser().getLogin())) {
            //When the user who called me, and select my video in camera options dialog, this function can not be called. this means that no need to show opponent video.
            return;
        }
        
        localVideoTrack.removeRenderer(localVideoTrack.getRenderer());
        if (isPeerToPeerCall) {
            remoteVideoTrack = videoTrack;
            setDuringCallActionBar();
            if (opponentVideoView == null) {
                opponentVideoView = (OpenCV_RTCRemoteVideoView) view.findViewById(R.id.remote_video_view);
            }

            if(remoteVideoTrack != null)
                fillVideoView(opponentVideoView, remoteVideoTrack, true);
            //updateVideoView(opponentVideoView, false);
            opponentVideoView.initListener(this);
            opponentVideoView.setOnClickListener(localViewOnClickListener);
        }
    }
    /////////////////////////////////////////    end    ////////////////////////////////////////////


    private void replaceUsersInAdapter(int position) {
        for (QBUser qbUser : allOpponents) {
            if (qbUser.getId() == userIDFullScreen) {
                opponentsAdapter.replaceUsers(position, qbUser);
                break;
            }
        }
    }

    @SuppressWarnings("ConstantConditions")

    private OpponentsFromCallAdapter.ViewHolder getViewHolderForOpponent(Integer userID) {
        OpponentsFromCallAdapter.ViewHolder holder = opponentViewHolders.get(userID);
        if (holder == null) {
            holder = findHolder(userID);
            if (holder != null) {
                opponentViewHolders.append(userID, holder);
            }
        }
        return holder;
    }

    private OpponentsFromCallAdapter.ViewHolder findHolder(Integer userID) {
        if (viewHolders == null) {
            Log.d(TAG, "viewHolders == null");
            return null;
        }
        for (OpponentsFromCallAdapter.ViewHolder childViewHolder : viewHolders) {
            Log.d(TAG, "getViewForOpponent holder user id is : " + childViewHolder.getUserId());
            if (userID.equals(childViewHolder.getUserId())) {
                return childViewHolder;
            }
        }
        return null;
    }

    private void fillVideoView(QBRTCSurfaceView videoView, QBRTCVideoTrack videoTrack, boolean remoteRenderer) {
        videoTrack.removeRenderer(videoTrack.getRenderer());
        videoTrack.addRenderer(new VideoRenderer(videoView));

        if (!remoteRenderer) {
            updateVideoView(videoView, isCurrentCameraFront);
        }
        Log.d(TAG, (remoteRenderer ? "remote" : "local") + " Track is rendering");
    }



    /**
     * @param userId set userId if it from fullscreen videoTrack
     */
    private void fillVideoView(int userId, QBRTCSurfaceView videoView, QBRTCVideoTrack videoTrack) {
        if (userId != 0) {
            userIDFullScreen = userId;
        }
        fillVideoView(videoView, videoTrack, true);
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror, RendererCommon.ScalingType scalingType) {
        Log.i(TAG, "updateVideoView mirror:" + mirror + ", scalingType = " + scalingType);
        surfaceViewRenderer.setScalingType(scalingType);
        surfaceViewRenderer.setMirror(mirror);
        surfaceViewRenderer.requestLayout();
    }

    private void setStatusForOpponent(int userId, final String status) {
        if (isPeerToPeerCall) {
            connectionStatusLocal.setText(status);
            return;
        }

        final OpponentsFromCallAdapter.ViewHolder holder = findHolder(userId);
        if (holder == null) {
            return;
        }

        holder.setStatus(status);
    }

    private void updateNameForOpponent(int userId, String newUserName) {
        OpponentsFromCallAdapter.ViewHolder holder = findHolder(userId);
        if (holder == null) {
            Log.d("UPDATE_USERS", "holder == null");
            return;
        }

        Log.d("UPDATE_USERS", "holder != null");
        holder.setUserName(newUserName);
    }

    private void setProgressBarForOpponentGone(int userId) {
        if (isPeerToPeerCall) {
            return;
        }
        final OpponentsFromCallAdapter.ViewHolder holder = getViewHolderForOpponent(userId);
        if (holder == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                holder.getProgressBar().setVisibility(View.GONE);
            }
        });
    }

    private void setBackgroundOpponentView(final Integer userId) {
        final OpponentsFromCallAdapter.ViewHolder holder = findHolder(userId);
        if (holder == null) {
            return;
        }

        if (userId != userIDFullScreen) {
            holder.getOpponentView().setBackgroundColor(Color.parseColor("#000000"));
        }
    }

    ///////////////////////////////  QBRTCSessionConnectionCallbacks ///////////////////////////

    @Override
    public void onStartConnectToUser(QBRTCSession qbrtcSession, Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_checking));
    }

    @Override
    public void onConnectedToUser(QBRTCSession qbrtcSession, final Integer userId) {
        connectionEstablished = true;
        setStatusForOpponent(userId, getString(R.string.text_status_connected));
        setProgressBarForOpponentGone(userId);
        drawingView.isDrawingPossible = true;
        //When the user called another, and select my video, then we don't need to show opponent video.
        //SharedPrefsHelper.getInstance().getQbUser().getLogin() this function returns current logged in user login information.
        if(ARChatApp.ant_camera_Name.equals(SharedPrefsHelper.getInstance().getQbUser().getLogin())){
            localVideoView.setVisibility(View.VISIBLE);
        }
        else{
            localVideoView.setVisibility(View.GONE);
            localVideoTrack.removeRenderer(localVideoTrack.getRenderer());
        }
        ((CallActivity)getActivity()).refreshFeaturesFromServer();
    }

    @Override
    public void onConnectionClosedForUser(QBRTCSession qbrtcSession, Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_closed));
        if (!isPeerToPeerCall) {
            Log.d(TAG, "onConnectionClosedForUser videoTrackMap.remove(userId)= " + userId);
            getVideoTrackMap().remove(userId);
            setBackgroundOpponentView(userId);
        }
        drawingView.isDrawingPossible = false;
    }

    @Override
    public void onDisconnectedFromUser(QBRTCSession qbrtcSession, Integer integer) {
        setStatusForOpponent(integer, getString(R.string.text_status_disconnected));
        drawingView.isDrawingPossible = false;
    }

    @Override
    public void onDisconnectedTimeoutFromUser(QBRTCSession qbrtcSession, Integer integer) {
        setStatusForOpponent(integer, getString(R.string.text_status_time_out));
        drawingView.isDrawingPossible = false;
    }

    @Override
    public void onConnectionFailedWithUser(QBRTCSession qbrtcSession, Integer integer) {
        setStatusForOpponent(integer, getString(R.string.text_status_failed));
        drawingView.isDrawingPossible = false;
    }

    @Override
    public void onError(QBRTCSession qbrtcSession, QBRTCException e) {
        drawingView.isDrawingPossible = false;

    }
    //////////////////////////////////   end     //////////////////////////////////////////


    /////////////////// Callbacks from CallActivity.QBRTCSessionUserCallback //////////////////////
    @Override
    public void onUserNotAnswer(QBRTCSession session, Integer userId) {
        setProgressBarForOpponentGone(userId);
        setStatusForOpponent(userId, getString(R.string.text_status_no_answer));
    }

    @Override
    public void onCallRejectByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
        setStatusForOpponent(userId, getString(R.string.text_status_rejected));
    }

    @Override
    public void onCallAcceptByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
        setStatusForOpponent(userId, getString(R.string.accepted));
    }

    @Override
    public void onReceiveHangUpFromUser(QBRTCSession session, Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_hang_up));
        Log.d(TAG, "onReceiveHangUpFromUser userId= " + userId);
        if (!isPeerToPeerCall) {
            if (userId == userIDFullScreen) {
                Log.d(TAG, "setAnotherUserToFullScreen call userId= " + userId);
                setAnotherUserToFullScreen();
            }
        }
    }
    //////////////////////////////////   end     //////////////////////////////////////////

    private void setAnotherUserToFullScreen() {
        if (opponentsAdapter.getOpponents().isEmpty()) {
            return;
        }
        int userId = opponentsAdapter.getItem(0);
//      get opponentVideoTrack - opponent's video track from recyclerView
        QBRTCVideoTrack opponentVideoTrack = getVideoTrackMap().get(userId);
        if (opponentVideoTrack == null) {
            Log.d(TAG, "setAnotherUserToFullScreen opponentVideoTrack == null");
            return;
        }

        //fillVideoView(userId, remoteFullScreenVideoView, opponentVideoTrack);
        Log.d(TAG, "fullscreen enabled");

        OpponentsFromCallAdapter.ViewHolder itemHolder = findHolder(userId);
        if (itemHolder != null) {
            opponentsAdapter.removeItem(itemHolder.getAdapterPosition());
            itemHolder.getOpponentView().release();
            Log.d(TAG, "onConnectionClosedForUser opponentsAdapter.removeItem= " + userId);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_fragment, menu);
        switchCameraMenuItem = menu.findItem(R.id.camera_switch);
        if(!ARChatApp.ant_camera_Name.equals(SharedPrefsHelper.getInstance().getQbUser().getLogin()))
            switchCameraMenuItem.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera_switch:
                Log.d("Conversation", "camera_switch");
                switchCamera(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOpponentsListUpdated(ArrayList<QBUser> newUsers) {
        super.onOpponentsListUpdated(newUsers);
        updateAllOpponentsList(newUsers);
        Log.d("UPDATE_USERS", "updateOpponentsList(), newUsers = " + newUsers);
        runUpdateUsersNames(newUsers);
    }

    private void updateAllOpponentsList(ArrayList<QBUser> newUsers) {

        for (int i = 0; i < allOpponents.size(); i++) {
            for (QBUser updatedUser : newUsers) {
                if (updatedUser.equals(allOpponents.get(i))) {
                    allOpponents.set(i, updatedUser);
                }
            }
        }
    }

    private void runUpdateUsersNames(final ArrayList<QBUser> newUsers) {
        //need delayed for synchronization with recycler view initialization
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (QBUser user : newUsers) {
                    Log.d("UPDATE_USERS", "foreach, user = " + user.getFullName());
                    updateNameForOpponent(user.getId(), user.getFullName());
                }
            }
        }, UPDATING_USERS_DELAY);
    }

    private enum CameraState {
        NONE,
        DISABLED_FROM_USER,
        ENABLED_FROM_USER
    }

    class LocalViewOnClickListener implements View.OnClickListener {
        private long lastFullScreenClickTime = 0L;

        @Override
        public void onClick(View v) {
            if ((SystemClock.uptimeMillis() - lastFullScreenClickTime) < FULL_SCREEN_CLICK_DELAY) {
                return;
            }
            lastFullScreenClickTime = SystemClock.uptimeMillis();

            if (connectionEstablished) {
                setFullScreenOnOff();
            }
        }

        private void setFullScreenOnOff() {
            if (actionBar.isShowing()) {
                hideToolBarAndButtons();
            } else {
                showToolBarAndButtons();
            }
        }

        private void hideToolBarAndButtons() {
            actionBar.hide();

            //localVideoView.setVisibility(View.INVISIBLE);
            rlAnnotationCtrl.setVisibility(View.VISIBLE);
            actionVideoButtonsLayout.setVisibility(View.GONE);

        }

        private void showToolBarAndButtons() {
            actionBar.show();

            //localVideoView.setVisibility(View.VISIBLE);
            rlAnnotationCtrl.setVisibility(View.INVISIBLE);
            actionVideoButtonsLayout.setVisibility(View.VISIBLE);

        }
    }

    private void runOnUiThread(Runnable runnable){
        mainHandler.post(runnable);
    }

    @Override
    public void drawingEnd(ArrayList<Point> ptArray, Point leftTop, Point rightBottom, int color) {
        //When the user end the drawing mark, this function will be called from UI/drawingView.
        //and set the marks information to localVideoView or opponentVideoView.
        if(drawingView.isDrawingPossible){
            if(ARChatApp.ant_camera_Name.equals(SharedPrefsHelper.getInstance().getQbUser().getLogin())){
                localVideoView.arrayTrackBox.add(new org.opencv.core.Rect(leftTop, rightBottom));
                localVideoView.mViewMode = localVideoView.START_CMT;
                localVideoView.freeDrawPoint = new ArrayList<Point>();
                localVideoView.freeDrawPoint = ptArray;
                localVideoView.isResetCMT = false;
                localVideoView.drawColor = color;
            }else{
                opponentVideoView.arrayTrackBox.add(new org.opencv.core.Rect(leftTop, rightBottom));
                opponentVideoView.mViewMode = localVideoView.START_CMT;
                opponentVideoView.freeDrawPoint = new ArrayList<Point>();
                opponentVideoView.freeDrawPoint = ptArray;
                opponentVideoView.isResetCMT = false;
                opponentVideoView.drawColor = color;
            }
        }
    }

    @Override
    public void getBitmapFromVideoRenderGUI(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //This bitmap is drawn marks in transparent bitmap. this function will be called every frame from OpenCV_RTCVideoView or OpenCV_RTCRemoteVideoView.
                opencvMarkDrawnImg.setImageBitmap(bmp);
            }
        });
    }

    @Override
    public void onGetRemoteMatData(final Mat mat, final float x, final float y, final float width, final float height, final float[] ptArray, final int color) {
        //When the user finish draw line, the mark will be saved to server.
        //After calling drawingEnd, in OpenCV_RTCRemoteVideoView or OpenCV_RTCVideoView, get the image of the frame and call this function.
        ARChatApp.mDatabase.child(ARChatApp.databaseID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FeatureItem featureItem = new FeatureItem();
                int length = (int) (mat.total() * mat.elemSize());
                byte buffer[] = new byte[length];
                mat.get(0, 0, buffer);
                featureItem.img_gray = Base64.encodeToString(buffer, Base64.NO_WRAP);
                featureItem.x = x;
                featureItem.y = y;
                featureItem.width = width;
                featureItem.height = height;
                featureItem.ptArray = ptArray;
                featureItem.color = color;
                featureItem.img_height = mat.height();
                featureItem.img_width = mat.width();
                ARChatApp.mDatabase.child(ARChatApp.databaseID)
                        .child(String.format("%d", (int)dataSnapshot.getChildrenCount())).setValue(featureItem);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }
}


