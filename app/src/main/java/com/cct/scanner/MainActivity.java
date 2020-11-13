package com.cct.scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import android.net.Uri;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity{
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private QR_node head = null;
    private QR_node tail = null;
    private int size = 0;
    private Spinner spinner;

    private BarcodeScannerOptions options;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE)
                        .build();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
        previewView = findViewById(R.id.preview_view);
        spinner = (Spinner) findViewById(R.id.spinner);
        final Button add_QR_button = (Button) findViewById(R.id.button4);
        add_QR_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // your handler code here
                LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
                Log.i("Amount of Children:", Integer.toString(layout.getChildCount()));
                if(layout.getChildCount()!=size){
                    layout.removeAllViews();
                    List<String> spinnerElements = new ArrayList<String>();
                    if(size !=0) {
                        QR_node current = head;
                        for (int i = 0; i < size; i++) {
                            TextView textView = new TextView(getApplicationContext());
                            textView.setTextSize(30);
                            textView.setText(current.getData());
                            layout.addView(textView);
                            spinnerElements.add(current.getData());
                            current = current.getNext();

                        }
                    }
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, spinnerElements);
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down vieww
                    spinner.setAdapter(spinnerArrayAdapter);
                }
            }
        });
        final Button remove_QR_button = (Button) findViewById(R.id.button);
        remove_QR_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                String unwanted_data = spinner.getSelectedItem().toString();
                if(head.remove(unwanted_data)){
                    refresh();
                }
            }
        });
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new Analyzer());


        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,imageAnalysis, preview);
    }

    public void refresh(){
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        Log.i("Amount of Children:", Integer.toString(layout.getChildCount()));
        if(layout.getChildCount()!=size) {
            layout.removeAllViews();
            List<String> spinnerElements = new ArrayList<String>();
            if (size != 0) {
                QR_node current = head;
                for (int i = 0; i < size; i++) {
                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextSize(30);
                    textView.setText(current.getData());
                    layout.addView(textView);
                    spinnerElements.add(current.getData());
                    current = current.getNext();

                }
            }
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, spinnerElements);
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down vieww
            spinner.setAdapter(spinnerArrayAdapter);
        }
    }
    /*
    public void onClick() {
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(...)).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        // insert your code here.
                    }
                    @Override
                    public void onError(ImageCaptureException error) {
                        // insert your code here.
                    }
                }
    }
    */

    private class Analyzer implements ImageAnalysis.Analyzer {

        @Override
        @androidx.camera.core.ExperimentalGetImage
        public void analyze(ImageProxy imageProxy) {
            //Log.i("Test", "Made it inside analyzer");
            InputImage image;
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                //Log.i("Test", "Non-null image");
                image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                BarcodeScanner scanner = BarcodeScanning.getClient(options);
                Task<List<Barcode>> result = scanner.process(image)
                        .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes) {

                                for (Barcode barcode : barcodes) {
                                    Rect bounds = barcode.getBoundingBox();
                                    Point[] corners = barcode.getCornerPoints();

                                    if(findViewById(R.id.button4).isPressed()) {
                                        String rawValue = barcode.getRawValue();
                                        if (head == null) {
                                            head = new QR_node(rawValue);
                                            tail = head;
                                            Log.i("Barcode Value", rawValue);
                                            size +=1;
                                        } else {
                                            if (!head.search(rawValue)) {
                                                QR_node new_tail = new QR_node(rawValue);
                                                tail.setNext(new_tail);
                                                tail = new_tail;
                                                Log.i("Barcode Value", rawValue);
                                                size +=1;
                                            }
                                        }
                                    }

                                    /*
                                    int valueType = barcode.getValueType();
                                    // See API reference for complete list of supported types
                                    switch (valueType) {
                                        case Barcode.TYPE_WIFI:
                                            String ssid = barcode.getWifi().getSsid();
                                            String password = barcode.getWifi().getPassword();
                                            int type = barcode.getWifi().getEncryptionType();
                                            break;
                                        case Barcode.TYPE_URL:
                                            String title = barcode.getUrl().getTitle();
                                            String url = barcode.getUrl().getUrl();
                                            break;
                                        case Barcode.TYPE_TEXT:
                                            //Log.i("Test", "Found text QR code");
                                            //Toast.makeText(getApplicationContext(), "Added new QR code.", Toast.LENGTH_SHORT).show();
                                            break;
                                    }

                                     */
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                                Log.e("Test", e.getMessage());
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Barcode>> task) {

                                mediaImage.close();
                                imageProxy.close();

                            }
                        });
                    }
                };

                // Pass image to an ML Kit Vision API
                // ...

            /*
            Context context = new Context();

            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.path("C:\\Users\\Cameron\\Downloads\\test.png").build();
            try {
                image = InputImage.fromFilePath(context, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            */


    }
    private class QR_node {
        String data;
        QR_node next;

        public QR_node(String QR_data){
            data = QR_data;
            next = null;
        }
        public boolean search(String new_QR_code){
            if(new_QR_code.equals(data)){
                return true;
            }
            if(next == null){
                return false;
            }
            else{
                return next.search(new_QR_code);
            }
        }
        public boolean remove(String unwanted_data){
            if(data.equals(unwanted_data)){
                head = next;
                size -= 1;
                if(head == null){
                    tail = null;
                }
                return true;
            }
            if(next!=null) {
                if (next.getData().equals(unwanted_data)) {
                    if(next.equals(tail)){
                        tail = this;
                    }
                    next=next.getNext();
                    size -= 1;
                    return true;
                } else{
                    next.remove(unwanted_data);
                }
            }
            return false;
        }

        public void setNext(QR_node new_tail){
            next = new_tail;
        }

        public QR_node getNext(){
            return next;
        }

        public String getData(){
            return data;
        }
    }
}
