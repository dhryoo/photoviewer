package com.sarriaroman.PhotoViewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Base64;
import android.view.View;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.UrlConnectionDownloader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Iterator;

import uk.co.senab.photoview.PhotoViewAttacher;

// public class PhotoActivity extends Activity implements GestureDetector.OnGestureListener
// , GestureDetector.OnDoubleTapListener
//
public class PhotoActivity extends Activity 
{
//    private GestureDetectorCompat mDetector;
    private GestureDetector gestureDetector = null;

    
    private PhotoViewAttacher mAttacher;

    private ImageView photo;

    private ImageButton closeBtn;
    private ImageButton shareBtn;
    private ProgressBar loadingBar;

    private TextView titleTxt;

    private String mImage;
    private String mTitle;
    private boolean mShare;
    private JSONObject mHeaders;
    private JSONObject pOptions;
    private File mTempImage;
    private int shareBtnVisibility;


    public static JSONArray mArgs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getApplication().getResources().getIdentifier("activity_photo", "layout", getApplication().getPackageName()));

        // Load the Views
        findViews();

        try {
            this.mImage = mArgs.getString(0);
            this.mTitle = mArgs.getString(1);
            this.mShare = mArgs.getBoolean(2);
            this.mHeaders = parseHeaders(mArgs.optString(5));
            this.pOptions = mArgs.optJSONObject(6);

            if( pOptions == null ) {
                pOptions = new JSONObject();
                pOptions.put("fit", true);
                pOptions.put("centerInside", true);
                pOptions.put("centerCrop", false);
            }

            //Set the share button visibility
            shareBtnVisibility = this.mShare ? View.VISIBLE : View.INVISIBLE;
            //mDetector = new GestureDetectorCompat(this,this);
                    // Set the gesture detector as the double tap
                    // listener.
            //mDetector.setOnDoubleTapListener(this);


            gestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() 
            { 
                @Override public boolean onDown(MotionEvent motionEvent) 
                { 
                    finish();
                    return true; 
                } 
                @Override public void onShowPress(MotionEvent motionEvent) 
                { 
                //    finish();
                } 
                @Override public boolean onSingleTapUp(MotionEvent motionEvent) 
                { 
                //    finish();
                    return true; 
                } 
                @Override public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) 
                { 
                //    finish();
                    return true; 
                } 

                @Override public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) 
                { 
                //    finish();
                    return true; 
                } 
                @Override public void onLongPress(MotionEvent motionEvent) 
                { 
                    finish();
                } 
            });



        } catch (JSONException exception) {
            shareBtnVisibility = View.INVISIBLE;
        }
        shareBtn.setVisibility(shareBtnVisibility);
        //Change the activity title
        if (!mTitle.equals("")) {
            titleTxt.setText(mTitle);
        }

        try {
            loadImage();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Set Button Listeners
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 24) {
                    try {
                        Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                        m.invoke(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Uri imageUri;
                if (mTempImage == null) {
                    mTempImage = getLocalBitmapFileFromView(photo);
                }

                imageUri = Uri.fromFile(mTempImage);

                if (imageUri != null) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);

                    sharingIntent.setType("image/*");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

                    startActivity(Intent.createChooser(sharingIntent, "Share"));
                }
            }
        });

    }

    /**
     * Find and Connect Views
     */
    private void findViews() {
        // Buttons first
        closeBtn = (ImageButton) findViewById(getApplication().getResources().getIdentifier("closeBtn", "id", getApplication().getPackageName()));
        shareBtn = (ImageButton) findViewById(getApplication().getResources().getIdentifier("shareBtn", "id", getApplication().getPackageName()));

        //ProgressBar
        loadingBar = (ProgressBar) findViewById(getApplication().getResources().getIdentifier("loadingBar", "id", getApplication().getPackageName()));
        // Photo Container
        photo = (ImageView) findViewById(getApplication().getResources().getIdentifier("photoView", "id", getApplication().getPackageName()));
        mAttacher = new PhotoViewAttacher(photo);

        // Title TextView
        titleTxt = (TextView) findViewById(getApplication().getResources().getIdentifier("titleTxt", "id", getApplication().getPackageName()));


        photo.setOnTouchListener(new View.OnTouchListener() 
        { 
            @Override public boolean onTouch(View view, MotionEvent motionEvent) 
            { 
                gestureDetector.onTouchEvent(motionEvent); return true; 
            } 
        });



    }

    /**
     * Get the current Activity
     *
     * @return
     */
    private Activity getActivity() {
        return this;
    }

    /**
     * Hide Loading when showing the photo. Update the PhotoView Attacher
     */
    private void hideLoadingAndUpdate() {
        photo.setVisibility(View.VISIBLE);
        loadingBar.setVisibility(View.INVISIBLE);
        shareBtn.setVisibility(shareBtnVisibility);

        mAttacher.update();
    }

    private RequestCreator setOptions(RequestCreator picasso) throws JSONException {
        if(this.pOptions.has("fit") && this.pOptions.optBoolean("fit")) {
            picasso.fit();
        }

        if(this.pOptions.has("centerInside") && this.pOptions.optBoolean("centerInside")) {
            picasso.centerInside();
        }

        if(this.pOptions.has("centerCrop") && this.pOptions.optBoolean("centerCrop")) {
            picasso.centerCrop();
        }

        return picasso;
    }

    /**
     * Load the image using Picasso
     */
    private void loadImage() throws JSONException {
        if (mImage.startsWith("http") || mImage.startsWith("file")) {
            Picasso picasso;
            if (mHeaders == null) {
                picasso = Picasso.with(PhotoActivity.this);
            } else {
                picasso = getImageLoader(this);
            }

            this.setOptions(picasso.load(mImage))
                    .into(photo, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            hideLoadingAndUpdate();
                        }

                        @Override
                        public void onError() {
                            Toast.makeText(getActivity(), "Error loading image.", Toast.LENGTH_LONG).show();

                            finish();
                        }
                    });
        } else if (mImage.startsWith("data:image")) {

            new AsyncTask<Void, Void, File>() {

                protected File doInBackground(Void... params) {
                    String base64Image = mImage.substring(mImage.indexOf(",") + 1);
                    return getLocalBitmapFileFromString(base64Image);
                }

                protected void onPostExecute(File file) {
                    mTempImage = file;
                    Picasso picasso = Picasso.with(PhotoActivity.this);

                    try {
                        setOptions(picasso.load(mTempImage))
                                .into(photo, new com.squareup.picasso.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        hideLoadingAndUpdate();
                                    }

                                    @Override
                                    public void onError() {
                                        Toast.makeText(getActivity(), "Error loading image.", Toast.LENGTH_LONG).show();

                                        finish();
                                    }
                                });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }.execute();

        } else {
            photo.setImageURI(Uri.parse(mImage));

            hideLoadingAndUpdate();
        }
    }

    public void onDestroy() {
        if (mTempImage != null) {
            mTempImage.delete();
        }
        super.onDestroy();
    }
    /*
    @Override
    public boolean onTouchEvent(MotionEvent event){

        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
            //    Log.d(DEBUG_TAG,"Action was DOWN");
                  System.out.println("down");
                finish();
                return true;
            default :

                return super.onTouchEvent(event);
        }
    }
    */


    //@Override
    /*
        public boolean onTouchEvent(MotionEvent event){
            if (this.mDetector.onTouchEvent(event)) {
                return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean onDown(MotionEvent event) {
            finish();
            return true;
        }
        */

        /*
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                float velocityX, float velocityY) {
            finish();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            finish();
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                float distanceY) {
            finish();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent event) {
            finish();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            finish();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            finish();
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent event) {
            finish();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            //Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
            finish();
            return true;
        }
        */


    public File getLocalBitmapFileFromString(String base64) {
        File file;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "share_image_" + System.currentTimeMillis() + ".png");
            file.getParentFile().mkdirs();
            FileOutputStream output = new FileOutputStream(file);
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            output.write(decoded);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            file = null;
        }
        return file;
    }

    /**
     * Create Local Image due to Restrictions
     *
     * @param imageView
     * @return
     */
    public File getLocalBitmapFileFromView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp;

        if (drawable instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        } else {
            return null;
        }

        // Store image to default external storage directory
        File file;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "share_image_" + System.currentTimeMillis() + ".png");
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();

        } catch (IOException e) {
            file = null;
            e.printStackTrace();
        }
        return file;
    }

    private JSONObject parseHeaders(String headerString) {
        JSONObject headers = null;

        // Short circuit if headers is empty
        if (headerString == null || headerString.length() == 0) {
            return headers;
        }

        // headers should never be a JSON array, only a JSON object
        try {
            headers = new JSONObject(headerString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return headers;
    }

    private Picasso getImageLoader(Context ctx) {
        Picasso.Builder builder = new Picasso.Builder(ctx);

        builder.downloader(new UrlConnectionDownloader(ctx) {
            @Override
            protected HttpURLConnection openConnection(Uri uri) throws IOException {
                HttpURLConnection connection = super.openConnection(uri);
                Iterator<String> keyIter = mHeaders.keys();
                String key = null;
                try {
                    while (keyIter.hasNext()) {
                        key = keyIter.next();
                        connection.setRequestProperty(key, mHeaders.getString(key));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return connection;
            }
        });

        return builder.build();
    }
}
