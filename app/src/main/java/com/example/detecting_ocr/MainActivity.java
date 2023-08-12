package com.example.detecting_ocr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/** @noinspection deprecation*/
public class MainActivity extends AppCompatActivity {
    ImageView image;
    TextView textresult;
    Button button;
    private static final int REQUEST_IMAGE_GALLERY = 1;
    private static final int REQUEST_IMAGE_CAMERA = 2;
    String currentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.imageselector);
        button = findViewById(R.id.buttonselector);
        textresult = findViewById(R.id.textresult);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

    }
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an option");
        String[] options = {"Camera", "Gallery"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    openCamera();
                } else if (which == 1) {
                    openGallery();
                }
            }
        });
        builder.show();
    }
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                  ex.printStackTrace();
            }


            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this,getPackageName()+ ".fileprovider",photoFile);
               cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAMERA);
              //  startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }


    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_GALLERY) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    image.setImageURI(selectedImageUri);
                   // getWindow().setFormat(PixelFormat.RGBA_8888);
                    performTextRecognition(selectedImageUri);

                }
            } else if (requestCode == REQUEST_IMAGE_CAMERA) {
                if (currentPhotoPath != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    image.setImageBitmap(bitmap);
                    performTextRecognition(Uri.fromFile(new File(currentPhotoPath)));
                }
            }
        }

    }
 


    private void performTextRecognition(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
            InputImage inputImage = InputImage.fromBitmap(bitmap,0);
            TextRecognizer textRecognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Task<Text> task= textRecognizer.process(inputImage);
            task.addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    String recognizedText = text.getText();
                    textresult.setText(recognizedText);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "..Text recognition failed..", Toast.LENGTH_LONG).show();

                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Text__recognition__failed", Toast.LENGTH_LONG).show();
        }
    }

}