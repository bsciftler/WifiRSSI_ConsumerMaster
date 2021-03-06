package com.example.bright.pendintent;
import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.WorkerThread;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.util.Log;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import org.tensorflow.lite.Interpreter;
import static android.net.wifi.WifiManager.*;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.support.annotation.WorkerThread;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.tensorflow.lite.Interpreter;
import android.net.wifi.WifiManager;
import android.view.View;

import static android.net.wifi.WifiManager.*;


import static android.content.Context.WIFI_SERVICE;



/** Interface to load TfLite model and provide predictions. */
public class  indoorLocatorClient {
    private static final String TAG = "indoorLocatorDemo";
    private static final String MODEL_PATH = "Kilanimodel.tflite";
    private static final String DIC_PATH = "WAPs.txt";
    private static final String LABEL_PATH = "Labels.txt";

    private static final int SENTENCE_LEN = 256;  // The maximum length of an input sentence.
    // Simple delimiter to split words.
    private static final String SIMPLE_SPACE_OR_PUNCTUATION = " |\\,|\\.|\\!|\\?|\n";
    /*
     * Reserved values in ImdbDataSet dic:
     * dic["<PAD>"] = 0      used for padding
     * dic["<START>"] = 1    mark for the start of a sentence
     * dic["<UNKNOWN>"] = 2  mark for unknown words (OOV)
     */
    //private static final String START = "<START>";
    //private static final String PAD = "<PAD>";
    //private static final String UNKNOWN = "<UNKNOWN>";


    WifiManager wifiManager;

    /** Number of results to show in the UI. */
    private static final int MAX_RESULTS = 3;

    private final Context context;
    private final Map<String, Integer> dic = new HashMap<>();
    private final List<String> labels = new ArrayList<>();
    private Interpreter tflite;



    //MainActivity mActivity;
    /** An immutable result returned by a TextClassifier describing what was classified. */
    public static class Result {
        /**
         * A unique identifier for what has been classified. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        /** Display name for the result. */
        private final String title;

        /** A sortable score for how good the result is relative to others. Higher should be better. */
        private final Float confidence;

        public Result(final String id, final String title, final Float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }
    }
    ;

    public indoorLocatorClient(Context context) {
        this.context = context;
    }

    /** Load the TF Lite model and dictionary so that the client can start classifying text. */
    @MainThread
    public void load() {
        loadModel();
        loadDictionary();
        loadLabels();
    }

    /** Load TF Lite model. */
    @MainThread
    private synchronized void loadModel() {
        try {
            ByteBuffer buffer = loadModelFile(this.context.getAssets());
            tflite = new Interpreter(buffer);
            Log.v(TAG, "TFLite model loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /** Load words dictionary. */
    @WorkerThread
    private synchronized void loadDictionary() {
        try {
            loadDictionaryFile(this.context.getAssets());
            Log.v(TAG, "Dictionary loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }/*

    /** Load labels. */
    @WorkerThread
    private synchronized void loadLabels() {
        try {
            loadLabelFile(this.context.getAssets());
            Log.v(TAG, "Labels loaded.");
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /** Free up resources as the client is no longer needed. */
    @WorkerThread
    public synchronized void unload() {
        tflite.close();
        dic.clear();
        labels.clear();
    }

    /** Classify an input string and returns the classification results. */


    @WorkerThread
    public synchronized List<Result> locate(List<String> bssidlists, List<Integer> rssiValues){
        //get data
        List bssidL, rssiL, dicList, input, myLoc;
        bssidL = new ArrayList();
        rssiL = new ArrayList();
        dicList = new ArrayList();
        input = new ArrayList();
        myLoc = new ArrayList();

        bssidL.addAll(bssidlists);
        rssiL.addAll(rssiValues);

        //Pre-processing input data
        //Removing any unknown bssid
        for (int i = 0; i < bssidL.size(); i++){
            if (dic.containsKey(bssidL.get(i)) == false) {
                bssidL.remove(i);
                rssiL.remove(i);
            }
        }

        //sorting the input in accordance with model and -150 in case a network that existed in the dictionary does not exist in the current reading
        dicList.addAll(dic.keySet());

        for (int i = 0; i < dicList.size(); i++){
            for (int j=0; j < dicList.size(); j++){
                String wapnumber = (dic.get(dicList.get(j))).toString();
                int number = Integer.parseInt(String.valueOf(wapnumber.replace("wap","")));
                if (number == i){
                    if (bssidL.contains(dicList.get(j)) == true){
                        int rssiValue = Integer.parseInt(String.valueOf(rssiL.get((bssidL.indexOf(dicList.get(j))))));
                    }
                    else{
                        input.add(-150);
                    }
                }
            }
        }
        // Run inference.
        Log.v(TAG, "Locating with TF Lite...");
        float[][] output = new float[1][labels.size()];
        tflite.run(input, output);

        final ArrayList<Result> results = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            results.add(new Result("" + i, labels.get(i), output[0][i]));
        }

        bssidL.clear();
        rssiL.clear();
        dicList.clear();
        input.clear();
        myLoc.clear();

        return results;
    }



    /** Load TF Lite model from assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    /** Load labels from assets. */
    private void loadLabelFile(AssetManager assetManager) throws IOException {
        try (InputStream ins = assetManager.open(LABEL_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(ins))) {
            // Each line in the label file is a label.
            while (reader.ready()) {
                labels.add(reader.readLine());
            }
        }
    }

    /** Load dictionary from assets. */

    private void loadDictionaryFile(AssetManager assetManager) throws IOException {
        try (InputStream ins = assetManager.open(DIC_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(ins))) {
            // Each line in the WAPS file has 3 coloumn, SSID, BSSID, WAP#
            while (reader.ready()) {
                List<String> line = Arrays.asList(reader.readLine().split(","));
                if (line.size() < 3) {
                    continue;
                }
                dic.put(line.get(1), Integer.parseInt(line.get(2)));
            }
        }
    }

    /** Pre-prosessing: tokenize and map the input words into a float array. */
    /*float[][] tokenizeInputText(String text) {
        float[] tmp = new float[SENTENCE_LEN];
        List<String> array = Arrays.asList(text.split(SIMPLE_SPACE_OR_PUNCTUATION));

        int index = 0;
        // Prepend <START> if it is in vocabulary file.
        if (dic.containsKey(START)) {
            tmp[index++] = dic.get(START);
        }

        for (String word : array) {
            if (index >= SENTENCE_LEN) {
                break;
            }
            tmp[index++] = dic.containsKey(word) ? dic.get(word) : (int) dic.get(UNKNOWN);
        }
        // Padding and wrapping.
        Arrays.fill(tmp, index, SENTENCE_LEN - 1, (int) dic.get(PAD));
        float[][] ans = {tmp};
        return ans;
    }*/

    Map<String, Integer> getDic() {
        return this.dic;
    }

    Interpreter getTflite() {
        return this.tflite;
    }

    List<String> getLabels() {
        return this.labels;
    }
}

