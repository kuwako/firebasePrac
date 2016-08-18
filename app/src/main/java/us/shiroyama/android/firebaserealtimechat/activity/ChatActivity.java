package us.shiroyama.android.firebaserealtimechat.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import us.shiroyama.android.firebaserealtimechat.R;
import us.shiroyama.android.firebaserealtimechat.fragment.MessageDeleteDialogFragment;
import us.shiroyama.android.firebaserealtimechat.helper.AnalyticsHelper;
import us.shiroyama.android.firebaserealtimechat.helper.MessageHelper;
import us.shiroyama.android.firebaserealtimechat.helper.RemoteConfigHelper;
import us.shiroyama.android.firebaserealtimechat.helper.StorageHelper;
import us.shiroyama.android.firebaserealtimechat.model.LoginInfo;
import us.shiroyama.android.firebaserealtimechat.model.Message;
import us.shiroyama.android.firebaserealtimechat.widget.ChatAdapter;

/**
 * Chat Activity
 *
 * DBのルール
 * {
 *   "rules": {
 *     ".read": false,
 *     ".write": false,
 *     "users": {
 *     ".read": "auth != null",
 *     "$user_id": {
 *       ".write": "auth != null && auth.uid === $user_id"
 *     }
 *   },
 *   "messages": {
 *     ".read": "auth != null",
 *     ".indexOn": ["timestamp"],
 *     "$message_id": {
 *       ".write": "(auth != null && auth.uid === newData.child('senderUid').val()) || (auth != null && auth.uid === data.child('senderUid').val())"
 *     }
 *   }
 * },
 */
@RuntimePermissions
public class ChatActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private static final String KEY_USER_ID = "key_user_id";

    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 0x01;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.message_body)
    EditText messageBody;

    @Inject
    AnalyticsHelper analyticsHelper;

    @Inject
    RemoteConfigHelper remoteConfigHelper;

    @Inject
    MessageHelper messageHelper;

    @Inject
    StorageHelper storageHelper;

    @Inject
    Realm realm;

    @Inject
    ChatAdapter chatAdapter;

    @Inject
    Bus bus;

    @OnClick(R.id.post_button)
    void onClickPost(Button button) {
        messageHelper.send(messageBody);
    }

    @OnClick(R.id.upload)
    void onClickUpload(ImageView view) {
        ChatActivityPermissionsDispatcher.showFilePickerWithCheck(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ChatActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(REQUEST_CODE_READ_EXTERNAL_STORAGE)
                .withFilter(Pattern.compile(".*\\.jpg$"))
                .start();
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showFilePickerDenied() {
        Snackbar.make(recyclerView, R.string.permission_error_external_storage, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            Log.d(TAG, "filePath: " + filePath);

            storageHelper.uploadImage(filePath);
        }
    }

    public static Intent newIntent(Context context, @NonNull String userId) {
        return new Intent(context, ChatActivity.class)
                .putExtra(KEY_USER_ID, userId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        getComponent().inject(this);

        if (!getIntent().hasExtra(KEY_USER_ID)) {
            throw new IllegalArgumentException("uid must be supplied.");
        }

        String uid = getIntent().getStringExtra(KEY_USER_ID);
        LoginInfo loginInfo = realm.where(LoginInfo.class)
                .equalTo("uid", uid)
                .findFirst();

        analyticsHelper.logOpenScreen(TAG);
        remoteConfigHelper.setBackgroundColor(recyclerView);

        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        chatAdapter.onCreate(messages, loginInfo);

        recyclerView.setAdapter(chatAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition = (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        // remove focus from EditText
        recyclerView.setOnTouchListener((v, event) -> {
            if (recyclerView.hasFocus()) {
                return false;
            }
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(recyclerView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            recyclerView.requestFocus();
            return true;
        });

        messageHelper.onCreate(messages, chatAdapter, swipeRefreshLayout, recyclerView, loginInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    protected void onPause() {
        bus.unregister(this);
        super.onPause();
    }

    @Override
    public void onRefresh() {
        if (messageHelper.isInitialized()) {
            messageHelper.onRefresh();
        }
        analyticsHelper.logSwipeRefresh();
    }

    @Override
    protected void onDestroy() {
        if (messageHelper.isInitialized()) {
            messageHelper.onDestroy();
        }
        super.onDestroy();
    }

    @Subscribe
    public void onClick(ChatAdapter.OnClickEvent event) {
        if (messageHelper.isInitialized()) {
            messageHelper.onClick(event.position);
        }
    }

    @Subscribe
    public void onLongClick(ChatAdapter.OnLongClickEvent event) {
        if (messageHelper.isInitialized()) {
            messageHelper.onLongClick(event.position);
        }
    }

    @Subscribe
    public void onItemDeleteYes(MessageDeleteDialogFragment.OnYesEvent event) {
        if (messageHelper.isInitialized()) {
            messageHelper.onItemDeleteYes(event.position);
        }
    }

    @Subscribe
    public void onUploadSuccess(StorageHelper.UploadSuccessEvent event) {
        if (messageHelper.isInitialized()) {
            messageHelper.onImageUploadSuccess(event);
        }
    }

    // eventbusでアップロードの成功を監視
    @Subscribe
    public void onUploadResult(MessageHelper.ImageUploadResultEvent event) {
        if (!event.success) {
            Snackbar.make(recyclerView, R.string.image_upload_error, Snackbar.LENGTH_SHORT).show();
            Log.e(TAG, event.error.getMessage(), event.error);
        }
    }

    @Subscribe
    public void onImageDeleteRequest(MessageHelper.ImageDeleteRequestEvent event) {
        storageHelper.deleteImage(event.fileName);
    }

}
