package us.shiroyama.android.firebaserealtimechat.helper;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.storage.StorageReference;
import com.squareup.otto.Bus;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;

/**
 * @author Fumihiko Shiroyama (fu.shiroyama@gmail.com)
 */
public class StorageHelper {
    private static final String TAG = StorageHelper.class.getSimpleName();

    private static final String IMG_DIR = "images";

    private final AppCompatActivity activity;

    private final StorageReference storageReference;

    private final Bus bus;

    @Inject
    public StorageHelper(AppCompatActivity activity, StorageReference storageReference, Bus bus) {
        this.activity = activity;
        this.storageReference = storageReference;
        this.bus = bus;
    }

    public void uploadImage(String filePath) {
        String fileName = UUID.randomUUID().toString();
        StorageReference target = storageReference
                .child(IMG_DIR)
                .child(fileName);

        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
            target.putStream(inputStream)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "upload succeeded.");
                        long totalBytes = taskSnapshot.getTotalByteCount();
                        Uri downloadUri = taskSnapshot.getDownloadUrl();
                        bus.post(new UploadSuccessEvent(fileName, totalBytes, downloadUri));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, e.getMessage(), e));
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void deleteImage(String fileName) {
        Log.d(TAG, "Image delete requested. fileName: " + fileName);
        StorageReference target = storageReference
                .child(IMG_DIR)
                .child(fileName);
        target.delete()
                .addOnSuccessListener(result -> Log.d(TAG, "image delete ok."))
                .addOnFailureListener(error -> Log.e(TAG, error.getMessage(), error));
    }

    public static class UploadSuccessEvent {
        public final String fileName;
        public final long totalBytes;
        public final Uri downloadUrl;

        public UploadSuccessEvent(String fileName, long totalBytes, Uri downloadUrl) {
            this.fileName = fileName;
            this.totalBytes = totalBytes;
            this.downloadUrl = downloadUrl;
        }
    }
}
