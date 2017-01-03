package com.adnet.archat.QuickSample.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.core.helper.Utils;
import com.adnet.archat.Core.utils.KeyboardUtils;
import com.adnet.archat.Core.utils.SharedPrefsHelper;
import com.adnet.archat.Core.utils.Toaster;
import com.adnet.archat.R;
import com.adnet.archat.QuickSample.services.CallService;
import com.adnet.archat.Consts;
import com.adnet.archat.QuickSample.utils.QBEntityCallbackImpl;
import com.adnet.archat.QuickSample.utils.ValidationUtils;
import com.quickblox.users.model.QBUser;

/**
 * Created by tereha on 12.04.16.
 */
public class LoginActivity extends BaseActivity {

    private String TAG = LoginActivity.class.getSimpleName();

    private EditText userNameEditText;
    private EditText userPasswordEdt;

    private QBUser userForSave;

    private Button btnLogin, btnSignUp;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initUI();
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.root_view_login_activity);
    }

    private void initUI() {
        setActionBarTitle(R.string.title_login_activity);
        userNameEditText = (EditText) findViewById(R.id.user_name);
        userNameEditText.addTextChangedListener(new LoginEditTextWatcher(userNameEditText));

        userPasswordEdt = (EditText) findViewById(R.id.chat_room_name);
        userPasswordEdt.addTextChangedListener(new LoginEditTextWatcher(userPasswordEdt));

        btnLogin = (Button)findViewById(R.id.btnLogIn);
        btnSignUp = (Button)findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnteredUserNameValid() && isEnteredPasswordValid()) {
                    hideKeyboard();
                    showProgressDialog(R.string.dlg_sign_in);
                    signInCreatedUser(createUserWithEnteredData(), false);
                }
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean isEnteredPasswordValid() {
        return ValidationUtils.isPasswordValid(this, userPasswordEdt);
    }

    private boolean isEnteredUserNameValid() {
        return ValidationUtils.isUserNameValid(this, userNameEditText);
    }

    private void hideKeyboard() {
        KeyboardUtils.hideKeyboard(userNameEditText);
        KeyboardUtils.hideKeyboard(userPasswordEdt);
    }

    private void loginToChat(final QBUser qbUser) {
        userForSave = qbUser;
        saveUserData(userForSave);
        startLoginService(qbUser);
    }

    private void startOpponentsActivity() {
        OpponentsActivity.start(LoginActivity.this, false);
        finish();
    }

    private void saveUserData(QBUser qbUser) {
        SharedPrefsHelper sharedPrefsHelper = SharedPrefsHelper.getInstance();
        sharedPrefsHelper.save(Consts.PREF_CURREN_ROOM_NAME, qbUser.getTags().get(0));
        sharedPrefsHelper.saveQbUser(qbUser);
    }

    private QBUser createUserWithEnteredData() {
        return createQBUserWithCurrentData(String.valueOf(userNameEditText.getText()),
                String.valueOf(userPasswordEdt.getText()));
    }

    private QBUser createQBUserWithCurrentData(String userName, String password) {
        QBUser qbUser = null;
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
            StringifyArrayList<String> userTags = new StringifyArrayList<>();
            userTags.add(getResources().getString(R.string.users_tag));

            qbUser = new QBUser();
            qbUser.setFullName(userName);
            qbUser.setLogin(userName);
            qbUser.setPassword(password);
            qbUser.setTags(userTags);
        }

        return qbUser;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Consts.EXTRA_LOGIN_RESULT_CODE) {
            hideProgressDialog();
            boolean isLoginSuccess = data.getBooleanExtra(Consts.EXTRA_LOGIN_RESULT, false);
            String errorMessage = data.getStringExtra(Consts.EXTRA_LOGIN_ERROR_MESSAGE);

            if (isLoginSuccess) {
                saveUserData(userForSave);

                signInCreatedUser(userForSave, false);
            } else {
                Toaster.longToast(getString(R.string.login_chat_login_error) + errorMessage);
                userNameEditText.setText(userForSave.getFullName());
                userPasswordEdt.setText(userForSave.getPassword());
            }
        }
    }

    private void signInCreatedUser(final QBUser user, final boolean deleteCurrentUser) {
        final String strPassword = user.getPassword();
        requestExecutor.signInUser(user, new QBEntityCallbackImpl<QBUser>() {
            @Override
            public void onSuccess(QBUser result, Bundle params) {

                    subscribeToPushes();
                    userForSave = result;
                    userForSave.setPassword(strPassword);
                    loginToChat(userForSave);
                    startOpponentsActivity();

            }

            @Override
            public void onError(QBResponseException responseException) {
                hideProgressDialog();
                Toaster.longToast(R.string.sign_in_error_with_error);
            }
        });
    }


    private void startLoginService(QBUser qbUser) {
        Intent tempIntent = new Intent(this, CallService.class);
        PendingIntent pendingIntent = createPendingResult(Consts.EXTRA_LOGIN_RESULT_CODE, tempIntent, 0);
        CallService.start(this, qbUser, pendingIntent);
    }

    private void subscribeToPushes() {
        if (googlePlayServicesHelper.checkPlayServicesAvailable(this)) {
            Log.d(TAG, "subscribeToPushes()");
            googlePlayServicesHelper.registerForGcm(Consts.GCM_SENDER_ID);
        }
    }

    private String getCurrentDeviceId() {
        return Utils.generateDeviceId(this);
    }

    private class LoginEditTextWatcher implements TextWatcher {
        private EditText editText;

        private LoginEditTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            editText.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
