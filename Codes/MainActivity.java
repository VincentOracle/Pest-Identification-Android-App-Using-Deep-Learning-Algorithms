package com.example.pestify;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.pestify.R;
import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private ImageView imageView;
    private TextView textView;
    private Button galleryButton;
    private Button cameraButton;
    private TextView descriptionTextView;
    private Map<String, String> pestInfo;

    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize pest information dictionary
        pestInfo = new HashMap<>();
        initializePestInfo();

        // Load the TFLite model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e("TFLite", "Error loading model", e);
        }

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        galleryButton = findViewById(R.id.galleryButton);
        cameraButton = findViewById(R.id.cameraButton);

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission();
            }
        });
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with opening the camera
            openCamera();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("New_Model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
                    predict(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bitmap);
                predict(bitmap);
            }
        }
    }

    private void predict(Bitmap bitmap) {
        // Preprocess the image
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // Perform inference
        float[][] outputArray = new float[1][23]; // Updated from 16 to 22 if 22 is the new number of classes
        tflite.run(inputBuffer, outputArray);

        // Get the predicted class
        int predictedClass = argmax(outputArray[0]);
        String[] pestNames = getResources().getStringArray(R.array.pest_names);
        String predictedPest = pestNames[predictedClass];
        String pestDescription = pestInfo.get(predictedPest.toLowerCase()); // Fetch description from dictionary

        textView.setText("Predicted Pest: " + predictedPest);
        descriptionTextView.setText(pestDescription);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224);
        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private int argmax(float[] array) {
        int best = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[best]) {
                best = i;
            }
        }
        return best;
    }
    private void initializePestInfo() {
        pestInfo.put("ants", "Ants are social insects that can invade homes and damage crops. They can also protect and farm aphids, contributing to plant damage. They are considered harmful. Recommended pesticides: Boric acid, diatomaceous earth.");
        pestInfo.put("aphids", "Aphids are small insects that feed on plant sap, causing damage to crops. They can stunt plant growth and transmit plant viruses. They are considered harmful. Recommended pesticides: Neem oil, insecticidal soap.");
        pestInfo.put("armyworm", "Armyworms are caterpillars that can devastate crops by consuming leaves and stems. They can cause extensive damage to crops. They are considered harmful. Recommended pesticides: Bacillus thuringiensis (Bt), pyrethroids.");
        pestInfo.put("bees", "Bees are important pollinators, but certain species can become pests by nesting in unwanted areas or causing damage to structures. They are considered harmful in specific contexts. Recommended management strategies: Relocation, exclusion techniques.");
        pestInfo.put("beetle", "Beetles are insects that can chew on plant leaves and flowers. They can defoliate plants and reduce crop yields. They are considered harmful. Recommended pesticides: Carbaryl, neonicotinoids.");
        pestInfo.put("bollworm", "Bollworms are moth larvae that infest the reproductive parts of plants, like flower buds and fruiting structures. They can damage fruits and reduce crop quality. They are considered harmful. Recommended pesticides: Bacillus thuringiensis (Bt), pyrethroids.");
        pestInfo.put("grasshopper", "Grasshoppers are herbivorous insects that feed on vegetation, including crops. They can consume large quantities of plant material, leading to crop loss. They are considered harmful. Recommended pesticides: Carbaryl, malathion.");
        pestInfo.put("mice", "Mice are rodents that can damage crops by feeding on seeds, grains, and plant material. They can also spread diseases to humans and animals. They are considered harmful. Recommended pesticides: Rodenticides, traps.");
        pestInfo.put("mites", "Mites are tiny arachnids that can damage plants by feeding on their sap. They can cause discoloration and deformation of leaves. They are considered harmful. Recommended pesticides: Insecticidal soap, neem oil.");
        pestInfo.put("mosquito", "Mosquitoes are flying insects that can transmit diseases to humans and animals. They pose health risks to humans and animals. They are considered harmful. Recommended pesticides: Mosquito repellents containing DEET, permethrin.");
        pestInfo.put("moth", "Moths, particularly certain species like clothes moths, can damage fabrics, stored grains, and other materials. They are considered harmful in specific contexts. Recommended management strategies: Pheromone traps, sealing food storage containers.");
        pestInfo.put("sawfly", "Sawflies are related to wasps and can damage plants by feeding on leaves. They can defoliate plants and reduce overall plant health. They are considered harmful. Recommended pesticides: Bacillus thuringiensis (Bt), pyrethroids.");
        pestInfo.put("slug", "Slugs are gastropod mollusks that feed on plant leaves, seedlings, and fruits. They can cause extensive damage to crops, especially in moist conditions. They are considered harmful. Recommended pesticides: Iron phosphate baits, diatomaceous earth.");
        pestInfo.put("snail", "Snails are gastropod mollusks similar to slugs, feeding on plants and causing damage to crops. They thrive in moist environments. They are considered harmful. Recommended pesticides: Iron phosphate baits, diatomaceous earth.");
        pestInfo.put("stem borer", "Stem borers are larvae that tunnel into plant stems, causing structural damage. They can weaken and kill plants by disrupting nutrient flow. They are considered harmful. Recommended pesticides: Carbaryl, neonicotinoids.");
        pestInfo.put("weevil", "Weevils are beetles known for infesting stored grains and seeds. They can cause significant economic losses by contaminating food supplies. They are considered harmful. Recommended pesticides: Pyrethroids, phosphine gas fumigation.");
        pestInfo.put("field cricket", "Field crickets are insects known for their loud chirping and are commonly found in fields and gardens. They feed on a wide variety of organic materials and can damage plants by feeding on seedlings and mature plants alike. Although generally not considered as harmful as other pests, their presence in large numbers can lead to significant plant damage. Recommended pesticides: Insecticidal soap, pyrethroids.");
        pestInfo.put("semilooper", "Semiloopers are moth larvae that are known for their looping movement when they crawl. They primarily feed on the leaves of a variety of crops and can cause significant defoliation. This can lead to reduced crop yields and affect plant health adversely. They are considered harmful pests in agricultural settings. Recommended pesticides: Bacillus thuringiensis (Bt) for organic control,broader spectrum insecticides.");
        pestInfo.put("stem girdler", "Stem girdlers are beetles that attack trees, particularly fruit and nut trees, by girdling twigs and branches. This action disrupts the flow of nutrients and water, causing the affected parts to die. The damage can weaken trees and significantly affect fruit production. They are considered harmful, especially in orchards. Recommended pesticides: pyrethroids (for adult beetles) and systemic insecticides like imidacloprid (for larvae) during active periods..");
        pestInfo.put("mealybug", "Mealybugs are small, soft-bodied insects covered with a white, waxy, cotton-like substance. They infest a wide range of plants, sucking sap and secreting honeydew which leads to sooty mold. They can cause yellowing of leaves, stunted growth, and in severe cases, plant death. Mealybugs are particularly problematic in greenhouse settings and on houseplants. They are considered harmful. Recommended pesticides: Insecticidal soaps, neonicotinoids, and horticultural oils.");
        pestInfo.put("pod borer", "Pod borers are moth larvae that target leguminous crops by boring into their pods, consuming the seeds inside. This damage not only reduces the yield but also the quality of the crops such as beans and peas. Pod borers can cause significant losses in agriculture, especially when infestations are heavy. They are considered harmful. Recommended pesticides: Bacillus thuringiensis (Bt), synthetic pyrethroids, and insect growth regulators.");
        pestInfo.put("Scopula emissaria", "Scopula emissaria, commonly known as the small blood-vein, is a moth that can be found in various habitats, including gardens and agricultural areas. While not typically a major pest, in certain contexts it can affect ornamental plants and potentially some crops by feeding on foliage. Monitoring and identifying the specific plant host can help in managing their population.Recommended pesticides: Bacillus thuringiensis (Bt) .");
        pestInfo.put("termites", "Termites are social insects that feed on cellulose, primarily found in wood, paper, and other plant materials. They can cause severe damage to buildings, furniture, and other wooden structures. Termites often go unnoticed until substantial damage has been done. They are considered very harmful in residential and commercial settings. Recommended pesticides: fipronil and imidacloprid, which are applied as soil treatments or barrier applications around structures to prevent and control infestations.");
    }


}
