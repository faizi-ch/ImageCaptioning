

package com.android.ImageCaptioning;

import android.app.Activity;
import android.os.Bundle;


public class CameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Camera2BasicFragment.TensorflowInitializationPhoneState(this);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MainActivity.newInstance())
                    .commit();
        }
    }

}
