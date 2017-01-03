package com.adnet.archat.QuickSample.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.adnet.archat.Consts;
import com.adnet.archat.R;

/**
 * Created by TanZhegui on 9/23/2016.
 */
public class SelectANT_CameraDialog {
    private Context mContext;
    private RadioButton rdMyCamera, rdOpponentCamera;
    private RadioGroup rdgrpCamera;
    private TextView tvBtnCancel, tvBtnOK;

    public interface SelectANT_CameraListener {
        void SelectedAnnotationCamera(String userCameraName);
    }

    final AlertDialog dialog;
    final SelectANT_CameraListener listener;

    public SelectANT_CameraDialog(final Context context, final String myLogin, final String opponentLogin, final SelectANT_CameraListener listener) {
        this.listener = listener;
        mContext = context;
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_annotation_select, null);
        dialog = new AlertDialog.Builder(context).create();

        rdMyCamera = (RadioButton)view.findViewById(R.id.rdMyCamera);
        rdOpponentCamera = (RadioButton)view.findViewById(R.id.rdOpponentCamera);
        rdOpponentCamera.setText(opponentLogin + "'s Camera");
        rdgrpCamera = (RadioGroup)view.findViewById(R.id.rdCameraType);
        rdgrpCamera.check(rdOpponentCamera.getId());

        tvBtnCancel = (TextView)view.findViewById(R.id.tvBtnCancel);
        tvBtnOK = (TextView)view.findViewById(R.id.tvBtnOK);
        tvBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        tvBtnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                if(rdMyCamera.isChecked())
                    listener.SelectedAnnotationCamera(myLogin);
                else
                    listener.SelectedAnnotationCamera(opponentLogin);
            }
        });
        // kill all padding from the dialog window
        dialog.setView(view, 0, 0, 0, 0);

    }

    public void show() {
        dialog.show();
    }
}
