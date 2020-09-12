package com.app.plant;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Interpreter tflite;
    public Bitmap bitmap=null;
    Uri imageuri;
    private  int imageSizeX;
    private  int imageSizeY;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f; // 1.0 -> 255.0
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f; //255.0 -> 1.0
    private TensorImage inputImageBuffer;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;


    TextView textView ;
    public ImageView imageView;
    Button btnTest ,btnDev , btnHelp , btnUpload , btnMap;
    private Handler handler = new Handler();
    static final int REQUEST_IMAGE_CAPTURE = 1;
    final String ASSOCIATED_AXIS_LABELS = "labels.txt";
    List<String> associatedAxisLabels = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnDev=findViewById(R.id.button3);
        btnTest=findViewById(R.id.btnTest);
        imageView =findViewById(R.id.ivUploadXrays);
        btnUpload=findViewById(R.id.button);



        //setup imageview
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmap==null) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), 12);
                }

            }
        });


        //setup dev btn
        btnDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://www.instagram.com/wasnik.ankur.358/?hl=en");
                Intent intent = new Intent(Intent.ACTION_VIEW , uri);
                intent.setPackage("com.instagram.android");

                try {
                    startActivity(intent);
                }
                catch (ActivityNotFoundException e){
                    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.instagram.com/wasnik.ankur.358/?hl=en")));
                }
            }
        });

        // setup Test button
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if (bitmap==null){
                        Snackbar.make(btnUpload,"Please Upload image." , Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    //initialize the model
                    try {
                        tflite = new Interpreter(loadModelFile(MainActivity.this));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //ready inputs to model
                    int imageTensorIndex = 0;
                    int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
                    imageSizeY = imageShape[1];
                    imageSizeX = imageShape[2];
                    DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

                    //ready output to model
                    int probabilityTensorIndex = 0;
                    int[] probabilityShape =tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
                    DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

                    inputImageBuffer = new TensorImage(imageDataType);
                    outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                    probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();
                    //load the image
                    inputImageBuffer = loadImage(bitmap);
                    //run the model and store pred in outputProbabilityBuffer
                    tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());

                    tflite.close();
                    tflite= null ;
                    showresult();
            }
        });

    }

    private void showresult(){

        // Snackbar.make( btnTest , "Getting Results... ",Snackbar.LENGTH_SHORT).show();
        try {
            associatedAxisLabels = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading label file", e);
        }

        TensorProcessor probabilityProcessor =
                new TensorProcessor.Builder().add(new NormalizeOp(0, 255)).build();

        if (null != associatedAxisLabels) {
            // Map of labels and their corresponding probability
           TensorLabel labels = new TensorLabel(associatedAxisLabels,
                   probabilityProcessor.process(outputProbabilityBuffer));
            // Create a map to access the result based on label
            Map<String, Float> floatMap = labels.getMapWithFloatValue();
            final float[] pred = outputProbabilityBuffer.getFloatArray() ;
            int entryWithMaxValue=0;
            for (int i=0 ; i<15 ; i++){
                if(pred[i]==1){
                    entryWithMaxValue=i ;
                    break;
                }
            }
            String op = "";
            switch (entryWithMaxValue){
                case 0 : op="Pepper__bell___Bacterial_spot"; break;
                case 1 : op="Pepper__bell___healthy"; break;
                case 2 : op="Potato___Early_blight"; break;
                case 3 : op="Potato___Late_blight"; break;
                case 4 : op="Potato___healthy"; break;
                case 5 : op="Tomato_Bacterial_spot"; break;
                case 6 : op="Tomato_Early_blight"; break;
                case 7 : op="Tomato_Late_blight"; break;
                case 8 : op="Tomato_Leaf_Mold"; break;
                case 9 : op="Tomato_Septoria_leaf_spot"; break;
                case 10 : op="Tomato_Spider_mites_Two_spotted_spider_mite"; break;
                case 11 : op="Tomato__Target_Spot"; break;
                case 12: op="Tomato__Tomato_YellowLeaf__Curl_Virus"; break;
                case 13: op="Tomato__Tomato_mosaic_virus"; break;
                case 14: op="Tomato_healthy"; break;


            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Result");
            builder.setMessage(op);
            builder.setCancelable(true);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

            //Toast.makeText(this, op.toString() , Toast.LENGTH_SHORT).show();
            bitmap=null;
            new Handler().postDelayed(new Runnable() {


                @Override

                public void run() {
                    imageView.setImageBitmap(null);
                }

            }, 1000); // wait for 5 seconds


        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==12 && resultCode==RESULT_OK && data!=null) {
            imageuri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageuri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode==REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK && data!=null){
            Bitmap thumbnail = data.getParcelableExtra("data");
            bitmap = thumbnail;
        }
    }
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }
    private TensorImage loadImage( Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();

        return imageProcessor.process(inputImageBuffer);
    }

}
