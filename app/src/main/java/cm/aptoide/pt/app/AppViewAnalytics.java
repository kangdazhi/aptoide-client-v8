package cm.aptoide.pt.app;

import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;
import cm.aptoide.pt.dataprovider.model.v7.store.Store;
import cm.aptoide.pt.download.DownloadAnalytics;
import cm.aptoide.pt.store.StoreAnalytics;
import cm.aptoide.pt.timeline.TimelineAnalytics;
import cm.aptoide.pt.view.share.NotLoggedInShareAnalytics;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pedroribeiro on 10/05/17.
 */

public class AppViewAnalytics {

  public static final String EDITORS_CHOICE_CLICKS = "Editors_Choice_Clicks";
  public static final String HOME_PAGE_EDITORS_CHOICE_FLURRY = "Home_Page_Editors_Choice";
  public static final String APP_VIEW_OPEN_FROM = "App_Viewed_Open_From";
  public static final String OPEN_APP_VIEW = "OPEN_APP_VIEW";
  public static final String APP_VIEW_INTERACT = "App_View_Interact";
  public static final String CLICK_INSTALL = "Clicked on install button";
  private static final String APPLICATION_NAME = "Application Name";
  private static final String APPLICATION_PUBLISHER = "Application Publisher";
  private static final String ACTION = "Action";
  private static final String APP_SHORTCUT = "App_Shortcut";
  private static final String TYPE = "type";
  private final DownloadAnalytics downloadAnalytics;
  private AnalyticsManager analyticsManager;
  private NavigationTracker navigationTracker;
  private TimelineAnalytics timelineAnalytics;
  private NotLoggedInShareAnalytics notLoggedInShareAnalytics;
  private BillingAnalytics billingAnalytics;
  private StoreAnalytics storeAnalytics;

  public AppViewAnalytics(DownloadAnalytics downloadAnalytics, AnalyticsManager analyticsManager,
      NavigationTracker navigationTracker, TimelineAnalytics timelineAnalytics,
      NotLoggedInShareAnalytics notLoggedInShareAnalytics, BillingAnalytics billingAnalytics,
      StoreAnalytics storeAnalytics) {
    this.downloadAnalytics = downloadAnalytics;
    this.analyticsManager = analyticsManager;
    this.navigationTracker = navigationTracker;
    this.timelineAnalytics = timelineAnalytics;
    this.notLoggedInShareAnalytics = notLoggedInShareAnalytics;
    this.billingAnalytics = billingAnalytics;
    this.storeAnalytics = storeAnalytics;
  }

  public void sendEditorsChoiceClickEvent(String packageName, String editorsBrickPosition) {
    analyticsManager.logEvent(
        createEditorsChoiceClickEventMap(navigationTracker.getPreviousScreen(), packageName,
            editorsBrickPosition), EDITORS_CHOICE_CLICKS, AnalyticsManager.Action.CLICK,
        getViewName(false));
    analyticsManager.logEvent(
        createEditorsClickEventMap(navigationTracker.getPreviousScreen(), packageName,
            editorsBrickPosition), HOME_PAGE_EDITORS_CHOICE_FLURRY, AnalyticsManager.Action.CLICK,
        getViewName(false));
  }

  private Map<String, Object> createEditorsClickEventMap(ScreenTagHistory previousScreen,
      String packageName, String editorsBrickPosition) {
    Map<String, Object> map = new HashMap<>();
    map.put("Application Name", packageName);
    map.put("Search Position", editorsBrickPosition);
    if (previousScreen != null && previousScreen.getFragment() != null) {
      map.put("fragment", previousScreen.getFragment());
    }
    return map;
  }

  private Map<String, Object> createEditorsChoiceClickEventMap(ScreenTagHistory previousScreen,
      String packageName, String editorsBrickPosition) {
    Map<String, Object> map = new HashMap<>();
    if (previousScreen != null && previousScreen.getFragment() != null) {
      map.put("fragment", previousScreen.getFragment());
    }
    map.put("package_name", packageName);
    map.put("position", editorsBrickPosition);
    return map;
  }

  public void sendAppViewOpenedFromEvent(String packageName, String appPublisher, String badge,
      boolean hasBilling, boolean hasAdvertising) {
    analyticsManager.logEvent(createAppViewedFromMap(navigationTracker.getPreviousScreen(),
        navigationTracker.getCurrentScreen(), packageName, appPublisher, badge, hasBilling,
        hasAdvertising), APP_VIEW_OPEN_FROM, AnalyticsManager.Action.CLICK, getViewName(false));
    analyticsManager.logEvent(createAppViewDataMap(navigationTracker.getPreviousScreen(),
        navigationTracker.getCurrentScreen(), packageName, hasBilling, hasAdvertising),
        OPEN_APP_VIEW, AnalyticsManager.Action.CLICK, getViewName(false));
  }

  private Map<String, Object> createAppViewDataMap(ScreenTagHistory previousScreen,
      ScreenTagHistory currentScreen, String packageName, boolean hasBilling,
      boolean hasAdvertising) {
    Map<String, String> packageMap = new HashMap<>();
    packageMap.put("package", packageName);
    Map<String, Object> data = new HashMap<>();
    data.put("app", packageMap);
    if (previousScreen != null) {
      data.put("previous_store", previousScreen.getStore());
    } else {
      data.put("previous_store", APP_SHORTCUT);
    }
    if (currentScreen != null) {
      data.put("previous_tag", currentScreen.getTag());
    } else {
      data.put("previous_tag", APP_SHORTCUT);
    }

    data.put("appcoins_type", mapAppCoinsInfo(hasBilling, hasAdvertising));

    return data;
  }

  private String mapAppCoinsInfo(boolean hasBilling, boolean hasAdvertising) {
    if (hasBilling && hasAdvertising) {
      return "AppCoins Ads IAB";
    } else if (hasBilling) {
      return "AppCoins IAB";
    } else if (hasAdvertising) {
      return "AppCoins Ads";
    }
    return "None";
  }

  private HashMap<String, Object> createAppViewedFromMap(ScreenTagHistory previousScreen,
      ScreenTagHistory currentScreen, String packageName, String appPublisher, String badge,
      boolean hasBilling, boolean hasAdvertising) throws NullPointerException {
    HashMap<String, Object> map = new HashMap<>();
    if (previousScreen != null) {
      if (previousScreen.getFragment() != null) {
        map.put("fragment", previousScreen.getFragment());
      }
      if (previousScreen.getStore() != null) {
        map.put("store", previousScreen.getStore());
      }
    }
    if (currentScreen != null) {
      if (currentScreen.getTag() != null) {
        map.put("tag", currentScreen.getTag());
      }
    }

    map.put("appcoins_type", mapAppCoinsInfo(hasBilling, hasAdvertising));

    map.put("package_name", packageName);
    map.put("application_publisher", appPublisher);
    map.put("trusted_badge", badge);
    return map;
  }

  public void sendOpenScreenshotEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Open Screenshot"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendOpenVideoEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Open Video"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendRateThisAppEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Rate This App"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendReadAllEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Read All"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.VIEW, getViewName(true));
  }

  public void sendFollowStoreEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Follow Store"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendOpenStoreEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Open Store"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendOtherVersionsEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Other Versions"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendReadMoreEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Read More"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendFlagAppEvent(String flagDetail) {
    analyticsManager.logEvent(createFlagAppEventData("Flag App", flagDetail), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendSimilarAppsInteractEvent(String type) {
    analyticsManager.logEvent(createSimilarAppsEventData(type), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  private Map<String, Object> createFlagAppEventData(String action, String flagDetail) {
    Map<String, Object> map = new HashMap<>();
    map.put(ACTION, action);
    map.put("flag_details", flagDetail);
    return map;
  }

  private Map<String, Object> createSimilarAppsEventData(String type) {
    Map<String, Object> map = new HashMap<>();
    map.put(ACTION, "Open App on Recommended for you");
    map.put("bundle_tag", type);
    return map;
  }

  public void sendBadgeClickEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Open Badge"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendAppShareEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "App Share"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.CLICK, getViewName(true));
  }

  public void sendRemoteInstallEvent() {
    analyticsManager.logEvent(createMapData(ACTION, "Install on TV"), APP_VIEW_INTERACT,
        AnalyticsManager.Action.INSTALL, getViewName(true));
  }

  public void clickOnInstallButton(GetAppMeta.App app) {
    try {
      HashMap<String, Object> map = new HashMap<>();

      map.put(APPLICATION_NAME, app.getPackageName());
      map.put(APPLICATION_PUBLISHER, app.getDeveloper()
          .getName());

      analyticsManager.logEvent(map, CLICK_INSTALL, AnalyticsManager.Action.CLICK,
          getViewName(true));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void clickOnInstallButton(String packageName, String developerName, String type) {
    HashMap<String, Object> map = new HashMap<>();
    map.put(TYPE, type);
    map.put(APPLICATION_NAME, packageName);
    map.put(APPLICATION_PUBLISHER, developerName);
    analyticsManager.logEvent(map, CLICK_INSTALL, AnalyticsManager.Action.CLICK, getViewName(true));
  }

  private Map<String, Object> createMapData(String key, String value) {
    final Map<String, Object> data = new HashMap<>();
    data.put(key, value);
    return data;
  }

  private String getViewName(boolean isCurrent) {
    return navigationTracker.getViewName(isCurrent);
  }

  public void setupDownloadEvents(Download download, int campaignId, String abTestGroup,
      AnalyticsManager.Action action) {
    downloadAnalytics.downloadStartEvent(download, campaignId, abTestGroup,
        DownloadAnalytics.AppContext.APPVIEW, action);
  }

  public void sendTimelineLoggedInInstallRecommendEvents(String packageName) {
    timelineAnalytics.sendRecommendedAppInteractEvent(packageName, "Recommend");
  }

  public void sendTimelineLoggedInInstallRecommendSkipEvents(String packageName) {
    timelineAnalytics.sendRecommendedAppInteractEvent(packageName, "Skip");
  }

  public void sendTimelineLoggedInInstallRecommendDontShowMeAgainEvents(String packageName) {
    timelineAnalytics.sendRecommendedAppInteractEvent(packageName, "Don't show again");
  }

  public void sendSuccessShareEvent() {
    notLoggedInShareAnalytics.sendShareSuccess();
  }

  public void sendFailedShareEvent() {
    notLoggedInShareAnalytics.sendShareFail();
  }

  public void sendLoggedInRecommendAppDialogShowEvent(String packageName) {
    timelineAnalytics.sendRecommendedAppImpressionEvent(packageName);
  }

  public void sendNotLoggedInRecommendAppDialogShowEvent(String packageName) {
    notLoggedInShareAnalytics.sendNotLoggedInRecommendAppImpressionEvent(packageName);
  }

  public void sendDownloadPauseEvent(String packageName) {
    downloadAnalytics.downloadInteractEvent(packageName, "pause");
  }

  public void sendDownloadCancelEvent(String packageName) {
    downloadAnalytics.downloadInteractEvent(packageName, "cancel");
  }

  public void sendPaymentViewShowEvent() {
    billingAnalytics.sendPaymentViewShowEvent();
  }

  public void sendStoreOpenEvent(Store store) {
    storeAnalytics.sendStoreOpenEvent("App View", store.getName(), true);
  }
}
