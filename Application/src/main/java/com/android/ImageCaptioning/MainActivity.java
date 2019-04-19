package com.android.ImageCaptioning;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Fragment;

public class MainActivity extends Fragment
        implements FragmentCompat.OnRequestPermissionsResultCallback  {

    private Button mButton;
    private ImageView imageView;
    private static final String IMAGE_DIRECTORY = "/Captioned Images";
    private int GALLERY = 1, CAMERA = 2;
    Bitmap bitmap;
    RelativeLayout captionLayout;
    View captionView;
    TextToSpeech tts;
    FloatingActionButton fab, fab1, fab2;
    TextView fab1TextView, fab2TextView;
    LinearLayout fab1Layout, fab2Layout;
    boolean isFABOpen;


    private TextView textView;
    private static final String MODEL_FILE = "file:///android_asset/merged_frozen_graph.pb";
    private static final String INPUT1 = "encoder/import/InputImage:0";
    private static final String OUTPUT_NODES = "DecoderOutputs.txt";
    private static final int NUM_TIMESTEPS = 22;
    private static final int IMAGE_SIZE = 299;
    private static final int IMAGE_CHANNELS = 3;
    private static final int[] DIM_IMAGE=new int[]{1, IMAGE_SIZE, IMAGE_SIZE, IMAGE_CHANNELS};
    private TensorFlowInferenceInterface inferenceInterface;
    static {
        System.loadLibrary("tensorflow_inference");
    }
    private String[] OutputNodes = null;
    private String[] WORD_MAP = null;

    public static MainActivity newInstance() {
        return new MainActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        requestMultiplePermissions();

        imageView = (ImageView) view.findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });

        textView = (TextView) view.findViewById(R.id.textView);
        textView.setBackgroundColor(Color.BLACK);
        inferenceInterface = InitSession();

        mButton = view.findViewById(R.id.button);
        /*mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.setEnabled(false);
                mButton.setText("Processing...");

                //textView.setText(new Camera2BasicFragment().runModel(bitmap));
                *//*Thread thread = new Thread(MainActivity.this);
                thread.start();*//*
                try {

                    final String text = runModel(bitmap);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(text);
                            //tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                            //speak(text);
                            //String path = saveImage();
                            mButton.setText("Describe Me");
                            mButton.setEnabled(true);

                        }
                    });
                } finally {

                }
            }
        });*/

        captionLayout = (RelativeLayout) view.findViewById(R.id.captionLayout);

        tts = new TextToSpeech(getActivity().getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //Toast.makeText(getActivity().getApplicationContext(), "This language is not supported", Toast.LENGTH_SHORT).show();
                        showToast("This language is not supported");
                    }
                    else{
                        Log.v("TTS","onInit succeeded");
                        //Lspeak("working");
                    }
                } else {
                    //Toast.makeText(getApplicationContext(), "Initialization failed", Toast.LENGTH_SHORT).show();
                    showToast("Initialization failed");
                }

            }
        });

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab1 = (FloatingActionButton) view.findViewById(R.id.fab1);
        fab2 = (FloatingActionButton) view.findViewById(R.id.fab2);
        fab1TextView = view.findViewById(R.id.fab1TextView);
        fab2TextView = view.findViewById(R.id.fab2TextView);
        fab1Layout = view.findViewById(R.id.fab1Layout);
        fab2Layout = view.findViewById(R.id.fab2Layout);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isFABOpen){
                    showFABMenu();
                }else{
                    closeFABMenu();
                }
            }
        });
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFABMenu();
                showSaveDialog();

            }
        });
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speak(textView.getText().toString());
            }
        });

    }
    private void showFABMenu(){
        isFABOpen=true;
        fab1TextView.setVisibility(View.VISIBLE);
        fab2TextView.setVisibility(View.VISIBLE);
        fab1Layout.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        fab2Layout.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
    }

    private void closeFABMenu(){
        isFABOpen=false;
        fab1TextView.setVisibility(View.INVISIBLE);
        fab2TextView.setVisibility(View.INVISIBLE);
        fab1Layout.animate().translationY(0);
        fab2Layout.animate().translationY(0);
    }
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(getContext());
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallary();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }
    private void showSaveDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(getContext());
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Save image with caption",
                "Save image without caption" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                fab.setVisibility(View.GONE);
                                fab1Layout.setVisibility(View.GONE);
                                fab2Layout.setVisibility(View.GONE);
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 0, 0);
                                textView.setLayoutParams(params);
                                String path = saveImage(true);
                                showToast("Image Saved in "+path);
                                params.setMargins(0, 0, 70, 0);
                                textView.setLayoutParams(params);

                                fab.setVisibility(View.VISIBLE);
                                fab1Layout.setVisibility(View.VISIBLE);
                                fab2Layout.setVisibility(View.VISIBLE);
                            }
                                break;
                            case 1: {
                                String path = saveImage(false);
                                showToast("Image Saved in "+path);
                            }
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }
    Uri image_uri;
    private void takePhotoFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), contentURI);
                    //String path = saveImage(bitmap);
                    //Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    //showToast("Image Saved!");
                    //saveImage();
                    imageView.setImageBitmap(bitmap);

                    doWork();

                } catch (IOException e) {
                    e.printStackTrace();
                    //Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                    showToast("Failed!");
                }
            }

        } else if (requestCode == CAMERA) {
            //bitmap = (Bitmap) data.getExtras().get("data");
            //imageView.setImageBitmap(bitmap);
            //saveImage(bitmap);
            //saveImage();
            //Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
            //showToast("Image Saved!");

            //String path = image_uri.getPath().toString();

            //imageView.setImageURI(image_uri);
            //showToast(path);
            bitmap = null;
            try {
                String path = getRealPathFromUri(getContext(), image_uri);
                //bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), path);
                File imgFile = new  File(path);
                if(imgFile.exists()){
                    bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    bitmap = getResizedBitmap(bitmap, 1280, 720);
                    imageView.setImageBitmap(bitmap);
                    doWork();

                }



            } catch (Exception e) {
                //e.printStackTrace();
                showToast(e.getMessage());
            }

            /*saveImage(bitmap);
            showToast("Image Saved!");
            showToast(image_uri.toString());*/
        }
    }

    public void doWork()
    {
        try {

            final String text = runModel(bitmap);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                    //tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                    //speak(text);
                    //String path = saveImage();
                    fab.setVisibility(View.VISIBLE);
                    fab1Layout.setVisibility(View.VISIBLE);
                    fab2Layout.setVisibility(View.VISIBLE);
                }
            });
        } finally {

        }
    }

    public String saveImage(boolean withCaption) {
        Bitmap myBitmap = bitmap;
        //View rootView = getView();
        if (withCaption)
        {
            captionView=captionLayout;
            captionView.setDrawingCacheEnabled(true);
            captionView.buildDrawingCache(true);
            myBitmap = Bitmap.createBitmap(captionView.getDrawingCache(true));
            captionView.setDrawingCacheEnabled(false);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    private void requestMultiplePermissions(){
        Dexter.withActivity(getActivity())
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                            showToast("All permissions are granted by user!");
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        //Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                        showToast("Error!");
                    }
                })
                .onSameThread()
                .check();
    }


    String[] LoadFile(String fileName){
        InputStream is = null;
        try {
            is = getActivity().getAssets().open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total.toString().split("\n");
    }
    TensorFlowInferenceInterface InitSession(){
        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getActivity().getAssets(), MODEL_FILE);
        OutputNodes = LoadFile(OUTPUT_NODES);
        WORD_MAP = LoadFile("idmap");
        return inferenceInterface;
    }

    String runModel(Bitmap imBitmap){
        return  GenerateCaptions(Preprocess(imBitmap));
    }

    float[] Preprocess(Bitmap imBitmap){
        imBitmap = Bitmap.createScaledBitmap(imBitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        float[] floatValues = new float[IMAGE_SIZE * IMAGE_SIZE * 3];

        imBitmap.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((float)((val >> 16) & 0xFF))/255;//R
            floatValues[i * 3 + 1] = ((float)((val >> 8) & 0xFF))/255;//G
            floatValues[i * 3 + 2] = ((float)((val & 0xFF)))/255;//B
        }
        return floatValues;
    }

    String GenerateCaptions(float[] imRGBMatrix){
        inferenceInterface.fillNodeFloat(INPUT1, DIM_IMAGE, imRGBMatrix);
        inferenceInterface.runInference(OutputNodes);

        String result = "";
        int temp[][]= new int[NUM_TIMESTEPS][1];
        for(int i = 0; i<NUM_TIMESTEPS; ++i) {
            inferenceInterface.readNodeInt(OutputNodes[i], temp[i]);
            if(temp[i][0] == 2/*</S>*/){
                return result;
            }
            result += WORD_MAP[temp[i][0]]+" ";
        }
        return null;
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    void speak(String s){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.v("TAG", "Speak new API");
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, bundle, null);
        } else {
            Log.v("TAG", "Speak old API");
            HashMap<String, String> param = new HashMap<>();
            param.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, param);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Don't forget to shutdown tts!
        if (tts != null) {
            Log.v("TAG", "onDestroy: shutdown TTS");
            tts.stop();
            tts.shutdown();
        }
    }
}
