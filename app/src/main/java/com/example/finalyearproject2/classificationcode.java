package com.example.finalyearproject2;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

public class classificationcode {




    protected Interpreter tflite;



    private static final int result_display = 1;
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    result_display,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> object_one, Map.Entry<String, Float> object_two) {
                            return (object_one.getValue()).compareTo(object_two.getValue());
                        }
                    });


    public static  String model_location;
    public static  String label_location;
    public static  String get_choice;

    public void initializename(){

        if(get_choice.equals("china")){
            model_location = "graphchina.tflite";
            label_location = "labelschina.txt";
        }
        else if(get_choice.equals("pakistan")){
           model_location = "graphpak.lite";
            label_location = "labelspk.txt";

        }else if(get_choice.equals("usa")) {
            model_location = "graphdollar.tflite";
            label_location = "labelsdollar.txt";
        }
    }


    private List<String> labelList;
    private static final int batch_size = 1;
    private static final int pixel_size = 3;
    private int[] bufvalue = new int[length_x * length_y];
    private ByteBuffer bytevalue = null;
    private float[][] output_tensorflow = null;

    classificationcode(Activity activity){
        try {
            initializename();
            tflite = new Interpreter(loadmodel(activity));
            labelList = loadlist(activity);
            bytevalue = ByteBuffer.allocateDirect(4 * batch_size * length_x * length_y * pixel_size);
            bytevalue.order(ByteOrder.nativeOrder());
            output_tensorflow = new float[1][labelList.size()];
            fprobarray = new float[fstage][labelList.size()];

        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
    }


    private List<String> loadlist(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader bufread = new BufferedReader(new InputStreamReader(activity.getAssets().open(label_location)));
        String one_line;
        while ((one_line = bufread.readLine()) != null) {
            labelList.add(one_line);
        }
        bufread.close();
        return labelList;
    }



    private MappedByteBuffer loadmodel(Activity activity) throws IOException {
        AssetFileDescriptor fdesc = activity.getAssets().openFd(model_location);
        FileInputStream inputStream = new FileInputStream(fdesc.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fdesc.getStartOffset();
        long declaredLength = fdesc.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    String classification(Bitmap bitmap) {
        if (tflite == null) {
           return "Uninitialized Classifier.";
        }
        conversion(bitmap);
        tflite.run(bytevalue, output_tensorflow);
        clearlabel();
        String textToShow = output_result();
        return textToShow;
    }


    private static final int fstage = 3;
    private float[][] fprobarray = null;
    private static final float ffactor = 0.4f;

    void clearlabel(){
        int num_labels =  labelList.size();
        for(int j=0; j<num_labels; ++j){
            fprobarray[0][j] += ffactor*(output_tensorflow[0][j] -
                    fprobarray[0][j]);
        }
        for (int i=1; i<fstage; ++i){
            for(int j=0; j<num_labels; ++j){
                fprobarray[i][j] += ffactor*(
                        fprobarray[i-1][j] -
                                fprobarray[i][j]);

            }
        }
        for(int j=0; j<num_labels; ++j){
            output_tensorflow[0][j] = fprobarray[fstage-1][j];
        }
    }


    public void close() {
        tflite.close();
        tflite = null;
    }


    static final int length_x = 224;
    static final int length_y = 224;

    private static final int imgmean = 128;
    private static final float imgstd = 128.0f;

    private void conversion(Bitmap bitmap) {
        if (bytevalue == null) {
            return;
        }
        bytevalue.rewind();
        bitmap.getPixels(bufvalue, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pic_pixel = 0;
        for (int i = 0; i < length_x; ++i) {
            for (int j = 0; j < length_y; ++j) {
                final int val = bufvalue[pic_pixel++];
                /*
                int red   = (rgb >>> 16) & 0xFF;
                int green = (rgb >>>  8) & 0xFF;
                int blue  = (rgb >>>  0) & 0xFF;*/
                bytevalue.putFloat((((val >> 16) & 0xFF)-imgmean)/imgstd);
                bytevalue.putFloat((((val >> 8) & 0xFF)-imgmean)/imgstd);
                bytevalue.putFloat((((val) & 0xFF)-imgmean)/imgstd);
            }
        }
    }


    private String output_result() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(new AbstractMap.SimpleEntry<>(labelList.get(i), output_tensorflow[0][i]));
            if (sortedLabels.size() > result_display) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();

        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
           // textToShow = String.format("\n%s: %4.2f",label.getKey(),label.getValue()) + textToShow;
            textToShow = label.getKey() + textToShow;

            if(label.getKey()!="5000Rs"&&label.getKey()!="5000Rsback"){
                if(label.getValue()<.98){
                    return "Searching";
                }
            }else if(label.getKey()=="5000Rs"||label.getKey()=="5000Rsback"){
                if(label.getValue()<1.00){
                    return "Searching";
                }
            }


            //Log.d("mylog","Value : "+label.getKey()+" = "+label.getValue());
        }
        return textToShow;
    }


}
