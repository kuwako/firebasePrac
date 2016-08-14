package us.shiroyama.android.firebaserealtimechat.di;

import dagger.Subcomponent;
import us.shiroyama.android.firebaserealtimechat.activity.ChatActivity;
import us.shiroyama.android.firebaserealtimechat.activity.LoginActivity;
import us.shiroyama.android.firebaserealtimechat.di.scope.ActivityScope;

// どうやらDagger側がこのクラスをインプリメントしたActivityComponentImplクラスをビルド時に作成しているっぽい。
// 要調査
@ActivityScope
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {
    void inject(LoginActivity activity);

    void inject(ChatActivity activity);
}
