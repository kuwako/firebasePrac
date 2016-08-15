package us.shiroyama.android.firebaserealtimechat.di;

import javax.inject.Singleton;

import dagger.Component;

// 依存関係グラフの頂点??
// 実態クラスがDaggerApplicationComponentという名前で生成される。
// 生成されたクラス実態クラスがBuilderクラスを持っていて、build()すればinterfaceで用意したメソッドが注入されたインスタンスを返してくれる。
@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {
    ActivityComponent activityComponent(ActivityModule module);
}
