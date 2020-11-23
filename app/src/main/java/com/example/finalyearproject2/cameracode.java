package com.example.finalyearproject2;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;


import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;
import java.util.Locale;


public class cameracode extends Fragment
        implements FragmentCompat.OnRequestPermissionsResultCallback{

    private AutoFitTextureView textureView;

    public static cameracode newInstance(){
        return new cameracode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cameracode, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    private int donevoice=1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        classifier = new classificationcode(getActivity());

        //Text to speech basic initilization
        tts=new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result= tts.setLanguage(Locale.ENGLISH);
                    if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.e("TTS","Language not Supported" );

                    }else {


                    }

                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.e("TTS","DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDOOONE" );
                            donevoice=0;
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });

                }else {
                    Log.e("TTS","Init Failed" );
                }




            }
        });


        run_background_thread();

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();

        //start background thread
        run_background_thread();
        //else condition run hogi jab Application resume hogi
        if (textureView.isAvailable()) {
            start_camera_usage(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        finish_camera_usage();
        quit_background_thread();
        super.onPause();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    /** Override Finished **/






    /** Permissions  Started**/


    private boolean check_permission =false;
    private int permission_req_code =1;

    //check if all permissions granted. return false agr koi c bhi aik granted na ho
    private boolean check_permissions_granted() {
        for(String Permission : input_permission()){
            if(ContextCompat.checkSelfPermission(getActivity(),Permission)!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }



    //sari permissions return krta jo require hon.
    private String[] input_permission(){
        Activity activity=getActivity();
        try {
            PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps=info.requestedPermissions;
            if(ps!=null && ps.length>0){
                return ps;
            }else {
                return new String[0];
            }

        } catch (PackageManager.NameNotFoundException e) {
            return new String[0];
        }
    }


    /** Permissions Finished **/





    /** Camera Code Started **/
    //camera start is function se
    private void start_camera_usage(int width, int height) {

        //function tb pura run hoga jab sari permissions granted hon
        if(!check_permissions_granted()&&!check_permission){
            FragmentCompat.requestPermissions(this,input_permission(), permission_req_code);
            return;
        }
        else {
            check_permission =true;
        }

        /// camera properties set
        set_camera_properties(width,height);
        //// screen py uska size
        configure_screen_transform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!camera_semaphore.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to object_lock camera opening.");
            }

            manager.openCamera(camera_id, stateCallback, bckgrnd_handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to object_lock camera opening.", e);
        }

    }




    private String camera_id;
    private CameraDevice cam_device;
    private int max_campreview_width=1920;
    private int max_campreview_height=1080;
    private Size camera_preview_size;
    private ImageReader img_reader;

    private void set_camera_properties(int width, int height) {
        Activity activity=getActivity();
        CameraManager manager=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);


        try {
            for(String cameraid : manager.getCameraIdList()){
                CameraCharacteristics characteristics=manager.getCameraCharacteristics(cameraid);
                Integer facing=characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing!=null && facing==CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                //output format / frame duration aur stalling ka btata.
                StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map==null){
                    continue;
                }
                //sbse barhay AREA vali aye gi
                Size Largest= Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new compare_area_size());
                img_reader=ImageReader.newInstance(Largest.getWidth(),Largest.getHeight(),ImageFormat.JPEG,2);

                //yha py dekhna k SENSOR aur Disply screen ki rotation same ya ni
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions=false;
                switch (displayRotation){
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if(sensorOrientation==90||sensorOrientation==270){
                            swappedDimensions=true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if(sensorOrientation==0||sensorOrientation==180){
                            swappedDimensions=true;
                        }
                        break;

                    default:
                        Log.e(TAG,"Display Rotation not Good");

                }

                Point displaySize=new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if(maxPreviewWidth>max_campreview_width){
                    maxPreviewWidth=max_campreview_width;
                }

                if(maxPreviewHeight>max_campreview_height){
                    maxPreviewHeight=max_campreview_height;
                }

                camera_preview_size=get_best_aspect_ratio(map.getOutputSizes(SurfaceTexture.class),rotatedPreviewWidth,rotatedPreviewHeight,maxPreviewWidth,maxPreviewHeight,Largest);


                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.texture_length_width(camera_preview_size.getWidth(), camera_preview_size.getHeight());
                } else {
                    textureView.texture_length_width(camera_preview_size.getHeight(), camera_preview_size.getWidth());
                }

                this.camera_id = cameraid;
                return;


            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

///get the best aspect ratio from different available ratios of camera .. best suited for screen
    private static Size get_best_aspect_ratio(Size[] choices,int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // all the ratio bigger than screen
        List<Size> bigEnough = new ArrayList<>();
        //all the ratio bigger than screen
        List<Size> notBigEnough = new ArrayList<>();


        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size screen_options : choices) {
            if (screen_options.getWidth() <= maxWidth && screen_options.getHeight() <= maxHeight && screen_options.getHeight() == screen_options.getWidth() * h / w) {
                if (screen_options.getWidth() >= textureViewWidth && screen_options.getHeight() >= textureViewHeight) {
                   //all the big ones added to list
                    bigEnough.add(screen_options);
                } else {
                    //smaller
                    notBigEnough.add(screen_options);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new compare_area_size());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new compare_area_size());
        } else {
            Log.e(TAG, "No suitable preview size");
            return choices[0];
        }


    }


    private CaptureRequest.Builder capture_request_builder;
    private CameraCaptureSession camera_capture_session;
    private CaptureRequest capture_preview_request;

    //function to link texture view and camera. set camera to textureview
    private void link_txtview_and_camera() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            //assert throws error if not correct
            assert texture != null;
           // if (texture==null) throw new AssertionError("Texture cannot be null");

            texture.setDefaultBufferSize(camera_preview_size.getWidth(), camera_preview_size.getHeight());

            Surface surface = new Surface(texture);

            capture_request_builder = cam_device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            capture_request_builder.addTarget(surface);

            cam_device.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == cam_device) {
                                return;
                            }

                            camera_capture_session = cameraCaptureSession;
                            try {
                                capture_request_builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                capture_preview_request = capture_request_builder.build();
                                camera_capture_session.setRepeatingRequest(capture_preview_request, captureCallback, bckgrnd_handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private static class compare_area_size implements Comparator<Size>{
        //signum return 0,-1,1 krta agr value 1 se barhi choti aur 0 ho to
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long)o1.getWidth()*o1.getHeight()-(long)o2.getWidth()*o2.getHeight());
        }
    }

    /** Camera Code Finished **/




    /*** Configure Started ****/

    private void configure_screen_transform(int tv_width, int tv_height) {
        Activity activity = getActivity();
        if (null == textureView || null == camera_preview_size || null == activity) {
            return;
        }
        //0 - no rotation

        //2 - 180 degrees rotation

        // 3 - 270 degrees rotation

        // 1 - 90 degrees rotation
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        //Matrix class holds a 3x3 matrix for transforming coordinates.
        Matrix matrix = new Matrix();
        //RectF holds four float coordinates for a rectangle
        RectF view_rect = new RectF(0, 0, tv_width, tv_height);
        RectF camera_rect = new RectF(0, 0, camera_preview_size.getHeight(), camera_preview_size.getWidth());
        //center x of view rectangle
        float center_x = view_rect.centerX();
        //center y of view rectangle
        float center_y = view_rect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            //adding dx to its left and right coordinates vice versa
            camera_rect.offset(center_x - camera_rect.centerX(), center_y - camera_rect.centerY());
            matrix.setRectToRect(view_rect, camera_rect, Matrix.ScaleToFit.FILL);
            //height ya width mn se jo barhi
            float scale = Math.max((float) tv_height / camera_preview_size.getHeight(), (float) tv_width / camera_preview_size.getWidth());
            matrix.postScale(scale, scale, center_x, center_y);
            //rotate
            matrix.postRotate(90 * (rotation - 2), center_x, center_y);
        } else if (Surface.ROTATION_180 == rotation) {
            //rotate
            matrix.postRotate(180, center_x, center_y);
        }
        textureView.setTransform(matrix);
    }


    private Semaphore camera_semaphore = new Semaphore(1);

    private void finish_camera_usage() {
        try {
            camera_semaphore.acquire();
            if (null != camera_capture_session) {
                camera_capture_session.close();
                camera_capture_session = null;
            }
            if (null != cam_device) {
                cam_device.close();
                cam_device = null;
            }
            if (null != img_reader) {
                img_reader.close();
                img_reader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted camera closing.", e);
        } finally {
            camera_semaphore.release();
        }
    }



    /*** Configure Ended ****/




    /** Threading Started **/

    private final Object object_lock = new Object();
    private boolean classifier_start = false;



    private Handler bckgrnd_handler;
    private HandlerThread bckgrnd_thread;


    private void run_background_thread() {
        //thread initilization
        bckgrnd_thread = new HandlerThread("Camera Thread");
        bckgrnd_thread.start();
        //handler initilizaltion
        bckgrnd_handler = new Handler(bckgrnd_thread.getLooper());
        // synchronized keyword guarantees that no more than one thread at the time can execute the code of the method.
        synchronized (object_lock) {
            classifier_start = true;
        }
        bckgrnd_handler.post(periodic_classify);
    }




    private void quit_background_thread() {
        bckgrnd_thread.quitSafely();
        try {
            bckgrnd_thread.join();
            bckgrnd_thread = null;
            bckgrnd_handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /** Threading Finished **/

    /** CLASSIFICATION STARTED **/
//runnable to run code on Main Thread / for voice
    private Runnable periodic_classify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (object_lock) {
                        if (classifier_start) {
                            classification_image();
                        }
                    }
                    bckgrnd_handler.post(periodic_classify);
                }
            };

    private classificationcode classifier;


    private void classification_image() {
        if (classifier == null || getActivity() == null || cam_device == null) {
            return;
        }
        Bitmap bitmap = textureView.getBitmap(224, 224);
        String textToShow = classifier.classification(bitmap);
        bitmap.recycle();

        if(textToShow!="Searching"&&!tts.isSpeaking()){
            voicefunction(textToShow);
        }
    }

    private TextToSpeech tts;


    public static float dol_value=0;
    public static float yuan_value=0;


    private void voicefunction(final String text){
        if (!tts.isSpeaking()) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    switch (text){
                        case "1000Rs":
                            tts.speak("Thousand Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+1000/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+1000/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "1000Rsback":
                            tts.speak("Thousand Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+1000/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+1000/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "100Rs":

                            tts.speak("Hundred Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+100/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+100/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }

                            break;
                        case "100Rsback":
                            tts.speak("Hundred Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+100/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+100/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }

                            break;
                        case "50Rs":
                            tts.speak("Fifty Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+50/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+50/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "50Rsback":
                            tts.speak("Fifty Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+50/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+50/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "10Rs":
                            tts.speak("Ten Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+10/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+10/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "10Rsback":
                            tts.speak("Ten Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+10/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+10/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "500Rs":
                            tts.speak("Five Hundred Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+500/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+500/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "500Rsback":
                            tts.speak("Five Hundred Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+500/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+500/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "20Rs":
                            tts.speak("Twenty Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+20/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+20/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "20Rsback":
                            tts.speak("Twenty Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+20/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+20/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5000Rs":
                            tts.speak("Five thousand Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+5000/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+5000/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5000Rsback":
                            tts.speak("Five thousand Rupees",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Dollar "+5000/dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+5000/yuan_value,TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        /////china
                        case "10Yuan":
                            tts.speak("Ten Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+10*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+10*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "10Yuanback":
                            tts.speak("Ten Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+10*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+10*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "1Yuan":
                            tts.speak("One Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+1*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+1*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "1Yuanback":
                            tts.speak("One Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+1*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+1*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5Yuan":
                            tts.speak("Five Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+5*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+5*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5Yuanback":
                            tts.speak("Five Yuan",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+5*yuan_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Dollar "+5*(yuan_value/dol_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        ////dollar
                        case "1Dollar":
                            tts.speak("One Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+1*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+1*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "1Dollarback":
                            tts.speak("One Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+1*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+1*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "2Dollar":
                            tts.speak("Two Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+2*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+2*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "2Dollarback":
                            tts.speak("Two Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+2*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+2*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5Dollar":
                            tts.speak("Five Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+5*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+5*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;
                        case "5Dollarback":
                            tts.speak("Five Dollar",TextToSpeech.QUEUE_ADD,null);
                            if(dol_value!=0.0&&yuan_value!=0.0){
                                tts.speak("Equal Pakistan Ruppee "+5*dol_value,TextToSpeech.QUEUE_ADD,null);
                                tts.speak("Equal Yuan "+5*(dol_value/yuan_value),TextToSpeech.QUEUE_ADD,null);
                            }
                            break;


                        default:
                            break;
                    }
                }
            });

        }


    }



    /** CLASSIFICATION ENDED **/



    /** Call backs Started **/

    private CameraCaptureSession.CaptureCallback captureCallback=new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,@NonNull  CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,@NonNull  TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };


    private CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            camera_semaphore.release();
            cam_device = camera;
            link_txtview_and_camera();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera_semaphore.release();
            camera.close();
            cam_device = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera_semaphore.release();
            camera.close();
            cam_device = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };


    private final TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            start_camera_usage(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configure_screen_transform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    /** Call Backs Ended **/


}
