/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 28/07/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid;

import android.content.Context;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.Rollback;
import cm.aptoide.pt.model.v7.Type;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.install.Installer;
import cm.aptoide.pt.v8engine.interfaces.FragmentShower;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import cm.aptoide.pt.v8engine.view.recycler.displayable.DisplayablePojo;
import rx.Observable;

/**
 * Created by sithengineer on 14/06/16.
 */
public class RollbackDisplayable extends DisplayablePojo<Rollback> {

  private Installer installManager;

  public RollbackDisplayable() {
  }

  public RollbackDisplayable(Installer installManager, Rollback pojo) {
    this(installManager, pojo, false);
  }

  private RollbackDisplayable(Installer installManager, Rollback pojo, boolean fixedPerLineCount) {
    super(pojo);
    this.installManager = installManager;
  }

  public Download getDownloadFromPojo() {
    return new DownloadFactory().create(getPojo());
  }

  @Override public Type getType() {
    return Type.ROLLBACK;
  }

  @Override public int getViewLayout() {
    return R.layout.rollback_row;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }

  public void install(FragmentShower context) {
    openAppview(context);
  }

  public Observable<Void> uninstall(Context context, Download appDownload) {
    return installManager.uninstall(context,
        appDownload.getFilesToDownload().get(0).getPackageName());
  }

  public void downgrade(FragmentShower context) {
    openAppview(context);
  }

  public void update(FragmentShower context) {
    openAppview(context);
  }

  public void openAppview(FragmentShower fragmentShower) {
    fragmentShower.pushFragmentV4(
        V8Engine.getFragmentProvider().newAppViewFragment(getPojo().getMd5()));
  }
}
