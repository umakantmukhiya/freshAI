package com.app.plant;
import androidx.annotation.MainThread;
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
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Interpreter tflite;
    public Bitmap bitmap=null;
    Uri imageuri;
    private  int imageSizeX;
    private  int imageSizeY;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private TensorImage inputImageBuffer;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;


    public ImageView imageView;
    Button btnTest ,btnDev  , btnUpload,btnFruit , btnTestFruit;
   // private Handler handler = new Handler();
    static final int REQUEST_IMAGE_CAPTURE = 1; //for auth
    final String ASSOCIATED_AXIS_LABELS = "labels.txt";
    final String ASSOCIATED_AXIS_LABELS_FRUITS = "label_apple.txt";
    final String APPLE_MODEL = "apple.tflite";
    List<String> associatedAxisLabels = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFruit = findViewById(R.id.buttonfruit);
        btnDev=findViewById(R.id.button3);
        btnTest=findViewById(R.id.btnTest);
        imageView =findViewById(R.id.ivUploadXrays);
        btnUpload=findViewById(R.id.button);
        btnTestFruit =findViewById(R.id.btnTestApple);



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

        btnFruit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmap==null) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), 8);
                }



            }
        });

btnTestFruit.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if (bitmap==null){
            Snackbar.make(btnUpload,"Please Upload image." , Snackbar.LENGTH_SHORT).show();
            return;
        }
        //initialize the model
        try {
            tflite = new Interpreter(loadModelFile(MainActivity.this,"Apple"));
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
        appleresult();
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

        // setup check / Test button
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if (bitmap==null){
                        Snackbar.make(btnUpload,"Please Upload image." , Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    //initialize the model
                    try {
                        tflite = new Interpreter(loadModelFile(MainActivity.this,"Not_Apple"));
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


    private void appleresult() {
        try {
            associatedAxisLabels = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS_FRUITS);
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
            //  Map<String, Float> floatMap = labels.getMapWithFloatValue();
            final float[] pred = outputProbabilityBuffer.getFloatArray();
            int entryWithMaxValue =-1;
            for (int i = 0; i < 6; i++) {
                if (pred[i] == 1) {
                    entryWithMaxValue = i;
                    break;
                }
            }
            String op = "";
            switch (entryWithMaxValue) {
                case 0:
                    op = "fresh Apples";
                    break;
                case 1:
                    op = "fresh Banana";
                    break;
                case 2:
                    op = "fresh Oranges";
                    break;
                case 3:
                    op = "rotten Apples";
                    break;
                case 4:
                    op = "rotten Banana";
                    break;
                case 5:
                    op = "rotten Oranges";
                    break;

            }
            Toast.makeText(this, op, Toast.LENGTH_SHORT).show();
            bitmap=null;
            new Handler().postDelayed(new Runnable() {


                @Override

                public void run() {
                    imageView.setImageBitmap(null);
                }

            }, 1000); // wait for 1 seconds
        }
    }

    private void showresult(){

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
          //  Map<String, Float> floatMap = labels.getMapWithFloatValue();
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
                case 0 : op="Pepper bell Bacterial_spot"; break;
                case 1 : op="Pepper bell healthy"; break;
                case 2 : op="Potato Early blight"; break;
                case 3 : op="Potato Late blight"; break;
                case 4 : op="Potato healthy"; break;
                case 5 : op="Tomato Bacterial spot"; break;
                case 6 : op="Tomato Early blight"; break;
                case 7 : op="Tomato Late blight"; break;
                case 8 : op="Tomato leaf Mold"; break;
                case 9 : op="Tomato Septoria_leaf spot"; break;
                case 10 : op="Tomato Spider mites "; break;
                case 11 : op="Tomato Target Spot"; break;
                case 12: op="Tomato YellowLeaf Curl Virus"; break;
                case 13: op="Tomato mosaic virus"; break;
                case 14: op="Tomato healthy"; break;
            }
            /* creating pathogen , host , */
            Map<Integer,String> pathogenMap = new HashMap<Integer, String>();
            Map<Integer,String> hostMap = new HashMap<Integer, String>();
            Map<Integer,String> remedyMap = new HashMap<Integer, String>();



            // hardcode values

            pathogenMap.put(new Integer(0)," Xanthomonas euvesicatoria , Xanthomonas perforans") ;
            hostMap.put(new Integer(0),"Pepper and tomato");
            remedyMap.put(new Integer(0),"1>Use disease-free seed\n" +
                    "\n" +
                    "2>Seed that has been hot water treated\n" +
                    "\n" +
                    "3>Purchase only certified &\n" +
                    "disease-free transplants\n" +
                    "\n" +
                    "4>Practice crop rotation");

            pathogenMap.put(new Integer(1),"NA") ; //healthy pepper bell
            hostMap.put(new Integer(1),"Pepper Bell");
            remedyMap.put(new Integer(1),"Take Care and Maintain");

            pathogenMap.put(new Integer(2),"Alternaria solani") ;
            hostMap.put(new Integer(2),"Potato and tomato");

            remedyMap.put(new Integer(2),"Avoid overhead irrigation and allow for sufficient aeration between plants to allow the foliage to dry as quickly as possible. Practice a 2-year crop rotation. That is, do not replant potatoes or other crops in this family for 2 years after a potato crop has been harvested.\n" +
                    "\n" +
                    "Read more at Gardening Know How: Potato Early Blight Treatment – Managing Potatoes With Early Blight https://www.gardeningknowhow.com/edible/vegetables/potato/potato-early-blight-treatment.htm");

            pathogenMap.put(new Integer(3)," Phytophthora infestans") ;
            hostMap.put(new Integer(3)," Potato, tomato");
            remedyMap.put(new Integer(3),"Plant resistant cultivars when available.\n" +
                    "Remove volunteers from the garden prior to planting and space plants far enough apart to allow for plenty of air circulation.\n" +
                    "Water in the early morning hours, or use soaker hoses, to give plants time to dry out during the day — avoid overhead irrigation.\n" +
                    "Destroy all tomato and potato debris after harvest (see Fall Garden Cleanup).");


            pathogenMap.put(new Integer(4),"NA") ; //healthy potato
            hostMap.put(new Integer(4),"Potato");
            remedyMap.put(new Integer(4),"Take Care and Maintain");

            pathogenMap.put(new Integer(5)," Xanthomonas euvesicatoria , Xanthomonas perforans") ;
            hostMap.put(new Integer(5),"Pepper and tomato");
            remedyMap.put(new Integer(5),"Sodium Hypochlorite");

            pathogenMap.put(new Integer(6),"Alternaria solani") ;
            hostMap.put(new Integer(6),"Potato and tomato");
            remedyMap.put(new Integer(6),"Bacillus Subtilis Hydroperoxyl");

            pathogenMap.put(new Integer(7)," Phytophthora infestans") ;
            hostMap.put(new Integer(7)," Potato, tomato");
            remedyMap.put(new Integer(7),"Actinovate Copper");

            pathogenMap.put(new Integer(8),"Cladosporium fulvum") ;
            hostMap.put(new Integer(8),"Tomato");
            remedyMap.put(new Integer(8),"Applying fungicides when symptoms first appear can reduce the spread of the leaf mold fungus significantly. Several fungicides are labeled for leaf mold control on tomatoes and can provide good disease control if applied to all the foliage of the plant, especially the lower surfaces of the leaves.");

            pathogenMap.put(new Integer(9)," Septoria lycopersici") ;
            hostMap.put(new Integer(9),"Tomato");
            remedyMap.put(new Integer(9),"Removing infected leaves. Remove infected leaves immediately, and be sure to wash your hands thoroughly before working with uninfected plants.\n" +
                    "Consider organic fungicide options. ...\n" +
                    "Consider chemical fungicides.");

            pathogenMap.put(new Integer(10)," Tetrany chus") ;
            hostMap.put(new Integer(10),"Tomato");
            remedyMap.put(new Integer(10),"The best way to begin treating for two-spotted mites is to apply a pesticide specific to mites called a miticide. Ideally, you should start treating for two-spotted mites before your plants are seriously damaged. Apply the miticide for control of two-spotted mites every 7 days or so");

            pathogenMap.put(new Integer(11)," Corynespora cassicola") ;
            hostMap.put(new Integer(11),"Tomato");
            remedyMap.put(new Integer(11),"Do not plant new crops next to older ones that have the disease.\n" +
                    "Plant as far as possible from papaya, especially if leaves have small angular spots\n" +
                    "Check all seedlings in the nursery, and throw away any with leaf spots.");

            pathogenMap.put(new Integer(12)," Begomo virus") ;
            hostMap.put(new Integer(12),"Tomato");
            remedyMap.put(new Integer(12),"Treatments that are commonly used for this disease include insecticides, hybrid seeds, and growing tomatoes under greenhouse conditions.");

            pathogenMap.put(new Integer(13)," Mosiac virus") ;
            hostMap.put(new Integer(13),"Tomato");
            remedyMap.put(new Integer(13),"There are no cures for viral diseases such as mosaic once a plant is infected. As a result, every effort should be made to prevent the disease from entering your garden. Fungicides will NOT treat this viral disease. Plant resistant varieties when available or purchase transplants from a reputable source.");

            pathogenMap.put(new Integer(14)," NA") ;
            hostMap.put(new Integer(14),"Tomato");
            remedyMap.put(new Integer(14),"Take Care and Maintain");


            Intent rintent = new Intent(MainActivity.this , Result_activity.class);
            rintent.putExtra("pathogen",pathogenMap.get(entryWithMaxValue)) ;
            rintent.putExtra("host" , hostMap.get(entryWithMaxValue));
            rintent.putExtra("headline",op);
            rintent.putExtra("remedy",remedyMap.get(entryWithMaxValue));

            startActivity(rintent);

            bitmap=null;
            new Handler().postDelayed(new Runnable() {


                @Override

                public void run() {
                    imageView.setImageBitmap(null);
                }

            }, 1000); // wait for 1 seconds


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

        if(requestCode==8 && resultCode==RESULT_OK && data!=null) {
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
    private MappedByteBuffer loadModelFile(Activity activity , String type) throws IOException {
        AssetFileDescriptor fileDescriptor = null;
        if(type == "Apple"){
             fileDescriptor = activity.getAssets().openFd(APPLE_MODEL);
        }
        else {
             fileDescriptor = activity.getAssets().openFd("model.tflite");
        }
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
