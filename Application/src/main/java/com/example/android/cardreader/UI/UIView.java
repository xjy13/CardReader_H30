package com.example.android.cardreader.UI;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class UIView {
    ProgressDialog progressDialog;
    Context ctx;

    public UIView(Context context) {
        this.ctx = context;
        this.progressDialog = new ProgressDialog(context);
    }

    public void getProgressDialog() {
        progressDialog.show(ctx, "",
                "Loading. Please wait...", true);
    }

    public void dismissProgressDialog() {
        progressDialog.dismiss();
    }


}
