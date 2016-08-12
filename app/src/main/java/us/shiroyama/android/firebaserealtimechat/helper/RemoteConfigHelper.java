package us.shiroyama.android.firebaserealtimechat.helper;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RemoteConfigHelper {
    private static final String TAG = RemoteConfigHelper.class.getSimpleName();

    private static final long CACHE_EXPIRATION = TimeUnit.HOURS.toSeconds(1);

    private final FirebaseRemoteConfig firebaseRemoteConfig;

    @Inject
    public RemoteConfigHelper(FirebaseRemoteConfig firebaseRemoteConfig) {
        this.firebaseRemoteConfig = firebaseRemoteConfig;
    }

    public void fetch() {
        long cacheExpiration = CACHE_EXPIRATION;
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "Fetch succeeded.");
                    firebaseRemoteConfig.activateFetched();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fetch failed.", e));
    }

    public void setBackgroundColor(View target) {
        // 背景色の取得
        // Tips: RemoteConfigは右上の保存ボタンを押さないと繁栄されない
        String colorHex = firebaseRemoteConfig.getString("chat_bg_color");
        Log.d(TAG, "colorHex: " + colorHex);
        int color = Color.parseColor(colorHex);
        target.setBackgroundColor(color);
    }
}
