package cm.aptoide.pt.crashreports;

import android.content.Context;
import cm.aptoide.pt.logger.Logger;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;
import lombok.Setter;

/**
 * Created by neuro on 09-12-2016.
 */

public class FabricCrashLogger implements CrashLogger {

  private final static String TAG = FabricCrashLogger.class.getName();

  private static final String LANGUAGE = "Language";

  //var with the language the app is set to
  @Setter private String language;

  public FabricCrashLogger(Context context, boolean isDisabled) {
    Fabric.with(context,
        new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(isDisabled).build())
            .build());
    Logger.d(TAG, "Setup of " + this.getClass().getSimpleName() + " complete.");
  }

  /**
   * Log crash exception to fabric.io
   *
   * @param throwable exception you want to send
   */
  @Override public void log(Throwable throwable) {
    if (Fabric.isInitialized()) {
      Crashlytics.setString(LANGUAGE, language);
    }
    Crashlytics.logException(throwable);
  }

  /**
   * Logs key-value pair in crashes
   *
   * @param key unique key to send on crash
   * @param value value you want associated with the key
   */
  @Override public void log(String key, String value) {
    if (Fabric.isInitialized()) {
      Crashlytics.setString(LANGUAGE, language);
    }
  }
}
