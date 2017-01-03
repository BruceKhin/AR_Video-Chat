package com.adnet.archat.QuickSample.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.view.View;

import com.adnet.archat.ARChatApp;
import com.adnet.archat.Core.gcm.GooglePlayServicesHelper;
import com.adnet.archat.Core.ui.activity.CoreBaseActivity;
import com.adnet.archat.Core.utils.ErrorUtils;
import com.adnet.archat.Core.utils.SharedPrefsHelper;
import com.adnet.archat.R;
import com.adnet.archat.QuickSample.util.QBResRequestExecutor;

/**
 * QuickBlox team
 */
public abstract class BaseActivity extends CoreBaseActivity {

    SharedPrefsHelper sharedPrefsHelper;
    private ProgressDialog progressDialog;
    protected GooglePlayServicesHelper googlePlayServicesHelper;
    protected QBResRequestExecutor requestExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestExecutor = ARChatApp.getInstance().getQbResRequestExecutor();
        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        googlePlayServicesHelper = new GooglePlayServicesHelper();
    }

    public void initDefaultActionBar() {
        String currentUserFullName = "";
        String currentRoomName = "AR Chat Annotations";

        if (sharedPrefsHelper.getQbUser() != null) {
            currentUserFullName = sharedPrefsHelper.getQbUser().getFullName();
        }

        setActionBarTitle(currentRoomName);
        setActionbarSubTitle(String.format(getString(R.string.subtitle_text_logged_in_as), currentUserFullName));
    }


    public void setActionbarSubTitle(String subTitle) {
        if (actionBar != null)
            actionBar.setSubtitle(subTitle);
    }

    public void removeActionbarSubTitle() {
        if (actionBar != null)
            actionBar.setSubtitle(null);
    }

    public void showProgressDialog(@StringRes int messageId) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            };
            progressDialog.setOnKeyListener(keyListener);
        }

        progressDialog.setMessage(getString(messageId));

        progressDialog.show();

    }

    public void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            };
            progressDialog.setOnKeyListener(keyListener);
        }

        progressDialog.setMessage(message);

        progressDialog.show();

    }

    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected void showErrorSnackbar(@StringRes int resId, Exception e,
                                     View.OnClickListener clickListener) {
        if (getSnackbarAnchorView() != null) {
            ErrorUtils.showSnackbar(getSnackbarAnchorView(), resId, e,
                    R.string.dlg_retry, clickListener);
        }
    }

    protected abstract View getSnackbarAnchorView();
}




