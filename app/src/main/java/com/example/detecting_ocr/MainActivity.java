package com.example.detecting_ocr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
                    Log.d("camera", "onActivityResult:  " + bitmap.getHeight() + " * " + bitmap.getHeight());
//                    3120 * 3120

                    // Apply sharpening filter
                    Bitmap grayscaleBitmap = applyGrayscaleFilter(bitmap); // Apply grayscale filter

                    Bitmap filteredBitmap = Bitmap_sharpen(grayscaleBitmap); image.setImageBitmap(grayscaleBitmap);
//                    image.setImageBitmap(bitmap);


                    // Convert the filteredBitmap to a Uri
                    Uri filteredImageUri = bitmapToUri(getApplicationContext(), filteredBitmap);

                    if (filteredImageUri != null) {
                        performTextRecognition(filteredImageUri);
                    }
                    //performTextRecognition(Uri.fromFile(new File(currentPhotoPath)));
                }
            }
        }

    }
    public Uri bitmapToUri(Context context, Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Image Title", null);
        return Uri.parse(path);
    }

//    public static Bitmap convertToBlackWhite(Bitmap bmp) {
//        int width = bmp.getWidth();
//        int height = bmp.getHeight();
//        int[] pixels = new int[width * height];
//        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
//
//        int alpha = 0xFF << 24; // ?bitmap?24?
//        for (int i = 0; i < height; i++) {
//            for (int j = 0; j < width; j++) {
//                int grey = pixels[width * i + j];
//
//                int red = ((grey & 0x00FF0000) >> 16);
//                int green = ((grey & 0x0000FF00) >> 8);
//                int blue = (grey & 0x000000FF);
//
//                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
//                grey = alpha | (grey << 16) | (grey << 8) | grey;
//                pixels[width * i + j] = grey;
//            }
//        }
//        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
//        return newBmp;
//    }
    public Bitmap Bitmap_sharpen(Bitmap bitmap) {

        int[] laplacian = new int[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 };

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        int pixR = 0;
        int pixG = 0;
        int pixB = 0;

        int pixColor = 0;

        int newR = 0;
        int newG = 0;
        int newB = 0;

        int idx = 0;
        float alpha = 0.3F;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 1, length = height - 1; i < length; i++) {
            for (int k = 1, len = width - 1; k < len; k++) {
                idx = 0;
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        pixColor = pixels[(i + n) * width + k + m];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);

                        newR = newR + (int) (pixR * laplacian[idx] * alpha);
                        newG = newG + (int) (pixG * laplacian[idx] * alpha);
                        newB = newB + (int) (pixB * laplacian[idx] * alpha);
                        idx++;
                    }
                }

                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));

                pixels[i * width + k] = Color.argb(255, newR, newG, newB);
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }
    public Bitmap applyGrayscaleFilter(Bitmap bitmap) {
        Bitmap grayscaleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Set saturation to 0 for grayscale
        Paint paint = new Paint();
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return grayscaleBitmap;
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

//                    List<String> namesToSearch = new ArrayList<>();
//                    namesToSearch.add("AASN0255");
//                    namesToSearch.add("7544ADPP065877");
//
//                    // Add more names as needed
//
//                    String foundName = findMatchingName(recognizedText, namesToSearch);
//
//                    if (foundName != null) {
//                        textresult.setText("Text found: " + foundName);
//                    } else {
//                        textresult.setText("No matching name found");
//                    }

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
//    private String findMatchingName(String recognizedText, List<String> namesToSearch) {
//        for (String name : namesToSearch) {
//            if (recognizedText.contains(name)) {
//                return name;
//            }
//        }
//        return null; // No matching name found
//    }
}