package cm.aptoide.pt.spotandshareapp.presenter;

import android.os.Bundle;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.spotandshareapp.SpotAndShareLocalUser;
import cm.aptoide.pt.spotandshareapp.SpotAndShareLocalUserManager;
import cm.aptoide.pt.spotandshareapp.view.SpotAndShareMainFragmentView;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by filipe on 08-06-2017.
 */

public class SpotAndShareMainFragmentPresenter implements Presenter {
  public static final int WRITE_SETTINGS_REQUEST_CODE_SEND = 3;
  public static final int WRITE_SETTINGS_REQUEST_CODE_RECEIVE = 4;
  public static final int WRITE_SETTINGS_REQUEST_CODE_SHARE_APTOIDE = 5;

  private SpotAndShareLocalUserManager spotAndShareUserManager;
  private SpotAndShareMainFragmentView view;

  public SpotAndShareMainFragmentPresenter(SpotAndShareMainFragmentView view,
      SpotAndShareLocalUserManager spotAndShareUserManager) {
    this.view = view;
    this.spotAndShareUserManager = spotAndShareUserManager;
  }

  @Override public void present() {

    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(created -> view.startSend())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(__ -> view.openAppSelectionFragment(true))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> err.printStackTrace());

    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(created -> view.startReceive())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(selection -> {
          view.openWaitingToReceiveFragment();
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> err.printStackTrace());

    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.RESUME))
        .flatMap(created -> view.editProfile())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(selection -> {
          view.openEditProfile();
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> err.printStackTrace());

    loadProfileInformationOnView();

    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.shareAptoideApk())
        .doOnNext(__ -> {
          view.openShareAptoideFragment();
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> err.printStackTrace());
  }

  @Override public void saveState(Bundle state) {

  }

  @Override public void restoreState(Bundle state) {

  }

  private SpotAndShareLocalUser getSpotAndShareProfileInformation() {
    return spotAndShareUserManager.getUser();
  }

  private void loadProfileInformationOnView() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(__ -> view.loadProfileInformation(getSpotAndShareProfileInformation()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(created -> {
        }, error -> error.printStackTrace());
  }
}