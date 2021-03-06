package app.steganosaurus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import app.steganosaurus.Utility.Const;
import app.steganosaurus.Utility.MediaManager;
import app.steganosaurus.Utility.Steganograph;
import steganosaurus.R;

/**
 * Activity class for steganography itself. Allows user to
 * select pictures to mix together.
 */
public class EncryptImageActivity extends AppCompatActivity {

    Bitmap selectedBasePicture;
    Bitmap selectedPictureToHide;
    Bitmap cameraPicture;
    Uri cameraImageUri;

    MediaManager mediaManager;
    Steganograph steganograph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt_image);

        mediaManager = new MediaManager(this);
        steganograph = new Steganograph();
    }

    /**
     * Callback. Takes selectedBasePicture and encrypts selectedPictureToHide in it
     * @param v the button that was clicked
     */
    public void encrypt(View v) {
        final String button_title = (String) ((Button)v).getText();
        final Context c = this;

        if(selectedBasePicture == null || selectedPictureToHide == null) {
            Toast.makeText(c, "Select two pictures to proceed ", Toast.LENGTH_LONG).show();
            return;
        }

        LoadingSpinnerAsync async = new LoadingSpinnerAsync();
        async.execute();
        final Bitmap resultingImage = steganograph.encodePicture(selectedBasePicture,selectedPictureToHide);
        async.isCompleted = true;

        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = getLayoutInflater().inflate(R.layout.popup_image_encrypt, null);
        //Imageviews
        ImageView imgvBefore = (ImageView)view.findViewById(R.id.result_popup_image_before);
        if (imgvBefore != null)
            imgvBefore.setImageBitmap(selectedBasePicture);
        ImageView imgvAfter = (ImageView)view.findViewById(R.id.result_popup_image_after);
        if (imgvAfter != null)
            imgvAfter.setImageBitmap(resultingImage);


        //Buttons
        Button b_ok = (Button)view.findViewById(R.id.encrypt_popup_go_back_btn);
        Button b_save = (Button)view.findViewById(R.id.encrypt_popup_save_btn);
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
            }
        });
        b_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaManager.SaveImageOn(resultingImage, getApplicationContext())){
                    Toast.makeText(c, "Image saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(c, "Image failed to save properly", Toast.LENGTH_SHORT).show();
                }
                dialog.hide();
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    /**
     * Callback. Allows user to take picture with the device's camera
     * request code depends on clicked button's id. MediaManager starts
     * the camera activity for result
     * @param v the button that was ciicked
     */
    public void takePicture(View v) {
        int requestCode = 0;
        int id = v.getId();

        if (id == R.id.encrypt_source_take_picture_btn)
            requestCode = Const.REQUEST_SOURCE_IMAGE_CAPTURE;
        else if (id == R.id.encrypt_hidden_take_picture_btn)
            requestCode = Const.REQUEST_HIDDEN_IMAGE_CAPTURE;

        cameraImageUri = mediaManager.takePicture(requestCode);
    }

    /**
     * Callback. allows user to select picture from their device
     * request code depends on the view that was clicked.
     * galleryManager starts the selection activity for result.
     * @param v the button that was clicked
     */
    public void getStoredPicturesFromDevice(View v) {
        int requestCode = 0;
        int id = v.getId();

        if (id == R.id.encrypt_source_image)
            requestCode = Const.PICK_SOURCE_IMAGE_REQUEST;
        else if (id == R.id.encrypt_hidden_image)
            requestCode = Const.PICK_HIDDEN_IMAGE_REQUEST;

        mediaManager.getStoredPicturesFromDevice(requestCode);
    }

    /**
     * Callback. Allows user to return to previous activity
     * @param v the button that was clicked
     */
    public void backHome(View v) {
        this.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK)
            switch (requestCode) {
            //Results for selecting a picture on the device
            case Const.PICK_SOURCE_IMAGE_REQUEST:
            case Const.PICK_HIDDEN_IMAGE_REQUEST:
                if (data.getData() != null) {
                    Bitmap selectedPicture = mediaManager.getSelectedPictureBitmap(requestCode, data);
                    if (requestCode == Const.PICK_SOURCE_IMAGE_REQUEST)
                        selectedBasePicture = selectedPicture;
                    else
                        selectedPictureToHide = selectedPicture;
                }
                break;

            //Result for taking a picture with hardware camera
            case Const.REQUEST_SOURCE_IMAGE_CAPTURE:
            case Const.REQUEST_HIDDEN_IMAGE_CAPTURE:
                try {
                    int id;
                    this.getContentResolver().notifyChange(cameraImageUri, null);
                    ContentResolver cr = this.getContentResolver();
                    cameraPicture = MediaStore.Images.Media.getBitmap(cr, cameraImageUri);
                    if (requestCode == Const.REQUEST_SOURCE_IMAGE_CAPTURE) {
                        id = R.id.encrypt_source_image;
                        selectedBasePicture = cameraPicture;
                    }
                    else {
                        id = R.id.encrypt_hidden_image;
                        selectedPictureToHide = cameraPicture;
                    }
                    ImageButton imgbtn = (ImageButton) findViewById(id);
                    if (imgbtn != null)
                        imgbtn.setImageBitmap(cameraPicture);
                } catch (Exception e) { e.printStackTrace(); }
                break;
        }
    }

    private class LoadingSpinnerAsync extends AsyncTask<Void, Void, Void> {
        public boolean isCompleted = false;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
            dialog = ProgressDialog.show(
                    EncryptImageActivity.this,
                    "Encrypting Data",
                    "Please wait...",
                    true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCompleted) { }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
        }
    }

}
