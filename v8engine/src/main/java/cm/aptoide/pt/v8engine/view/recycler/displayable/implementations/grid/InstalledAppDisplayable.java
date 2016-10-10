/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 25/05/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid;

import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.model.v7.Type;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.view.recycler.displayable.DisplayablePojo;

/**
 * Created by neuro on 17-05-2016.
 */
public class InstalledAppDisplayable extends DisplayablePojo<Installed> {

  public InstalledAppDisplayable() {
  }

  public InstalledAppDisplayable(Installed pojo) {
    super(pojo);
  }

  @Override public Type getType() {
    return Type.INSTALLED;
  }

  @Override public int getViewLayout() {
    return R.layout.installed_row;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }
}
