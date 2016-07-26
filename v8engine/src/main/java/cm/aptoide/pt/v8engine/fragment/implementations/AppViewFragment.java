/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 26/07/2016.
 */

package cm.aptoide.pt.v8engine.fragment.implementations;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.trello.rxlifecycle.FragmentEvent;

import java.util.LinkedList;
import java.util.List;

import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Scheduled;
import cm.aptoide.pt.dataprovider.util.DataproviderUtils;
import cm.aptoide.pt.dataprovider.ws.v2.aptwords.GetAdsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.GetAppRequest;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.downloadmanager.DownloadServiceHelper;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v2.GetAdsResponse;
import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.model.v7.GetAppMeta;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.GridRecyclerFragment;
import cm.aptoide.pt.v8engine.install.InstallManager;
import cm.aptoide.pt.v8engine.install.download.DownloadInstallationProvider;
import cm.aptoide.pt.v8engine.interfaces.AppMenuOptions;
import cm.aptoide.pt.v8engine.interfaces.Scrollable;
import cm.aptoide.pt.v8engine.model.MinimalAd;
import cm.aptoide.pt.v8engine.util.AppBarStateChangeListener;
import cm.aptoide.pt.v8engine.util.AppUtils;
import cm.aptoide.pt.v8engine.util.SearchUtils;
import cm.aptoide.pt.v8engine.util.StoreThemeEnum;
import cm.aptoide.pt.v8engine.util.ThemeUtils;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewDescriptionDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewDeveloperDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewFlagThisDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewInstallDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewRateAndCommentsDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewScreenshotsDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewStoreDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewSuggestedAppsDisplayable;
import io.realm.Realm;
import lombok.Cleanup;
import lombok.Getter;
import rx.functions.Action0;

/**
 * Created by sithengineer on 04/05/16.
 */
public class AppViewFragment extends GridRecyclerFragment implements Scrollable, AppMenuOptions {

	public static final int VIEW_ID = R.layout.fragment_app_view;

	//
	// constants
	//
	private static final String TAG = AppViewFragment.class.getSimpleName();
	private static final String BAR_EXPANDED = "BAR_EXPANDED";
	// FIXME restoreInstanteState doesn't work in this case
	private final Bundle memoryArgs = new Bundle();
	//private static final String TAG = AppViewFragment.class.getName();
	//
	// vars
	//
	private AppViewHeader header;
	//	private GetAppMeta.App app;
	private long appId;
	private Scheduled scheduled;
	private String storeTheme;
	//
	// static fragment default new instance method
	//
	private MinimalAd minimalAd;
	// Stored to postpone ads logic
	private AppViewInstallDisplayable appViewInstallDisplayable;
	private InstallManager installManager;

	private Action0 unInstallAction;
	private MenuItem uninstallMenuItem;
	private DownloadServiceHelper downloadManager;

	public static AppViewFragment newInstance(long appId) {
		Bundle bundle = new Bundle();
		bundle.putLong(BundleKeys.APP_ID.name(), appId);

		AppViewFragment fragment = new AppViewFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	public static AppViewFragment newInstance(long appId, String storeTheme) {
		Bundle bundle = new Bundle();
		bundle.putLong(BundleKeys.APP_ID.name(), appId);
		bundle.putString(StoreFragment.BundleCons.STORE_THEME, storeTheme);
		AppViewFragment fragment = new AppViewFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	public static AppViewFragment newInstance(GetAdsResponse.Ad ad) {
		Bundle bundle = new Bundle();
		bundle.putLong(BundleKeys.APP_ID.name(), ad.getData().getId());
		bundle.putParcelable(BundleKeys.MINIMAL_AD.name(), new MinimalAd(ad));

		AppViewFragment fragment = new AppViewFragment();
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final PermissionManager permissionManager = new PermissionManager();
		downloadManager = new DownloadServiceHelper(AptoideDownloadManager.getInstance(), permissionManager);
		installManager = new InstallManager(permissionManager, getContext().getPackageManager(),
				new DownloadInstallationProvider(downloadManager));
	}

	@Override
	public void loadExtras(Bundle args) {
		super.loadExtras(args);
		appId = args.getLong(BundleKeys.APP_ID.name());
		minimalAd = args.getParcelable(BundleKeys.MINIMAL_AD.name());
		storeTheme = args.getString(StoreFragment.BundleCons.STORE_THEME);
	}

	private void setupObservables(GetApp getApp) {
		// For stores subscription
		Database.StoreQ.getAll(realm).asObservable().compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW)).subscribe(stores -> {
			if (Database.StoreQ.get(getApp.getNodes().getMeta().getData().getStore().getId(), realm) != null) {
				adapter.notifyDataSetChanged();
			}
		});

		// For install actions
		Database.RollbackQ.getAll(realm).asObservable().compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW)).subscribe(rollbacks -> {
			adapter.notifyDataSetChanged();
		});

		// TODO: 27-05-2016 neuro install actions, not present in v7
	}

	private void setupDisplayables(GetApp getApp) {
		LinkedList<Displayable> displayables = new LinkedList<>();

		GetAppMeta.App app = getApp.getNodes().getMeta().getData();

		displayables.add(appViewInstallDisplayable = new AppViewInstallDisplayable(installManager, getApp, minimalAd != null ? minimalAd.getCpdUrl() : null,
				downloadManager));
		displayables.add(new AppViewStoreDisplayable(getApp));
		displayables.add(new AppViewRateAndCommentsDisplayable(getApp));
		displayables.add(new AppViewScreenshotsDisplayable(app));
		displayables.add(new AppViewDescriptionDisplayable(getApp));
		displayables.add(new AppViewFlagThisDisplayable(getApp));
		displayables.add(new AppViewSuggestedAppsDisplayable(getApp));
		displayables.add(new AppViewDeveloperDisplayable(getApp));

		setDisplayables(displayables);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void load(boolean refresh, Bundle savedInstanceState) {
		GetAppRequest.of(appId).execute(getApp -> {
			if (minimalAd == null) {
				GetAdsRequest.ofAppview(getApp.getNodes().getMeta().getData().getPackageName(), getApp.getNodes().getMeta().getData().getStore().getName())
						.execute(getAdsResponse -> {
							List<GetAdsResponse.Ad> ads = getAdsResponse.getAds();
							if (ads == null || ads.get(0) == null) {
								return;
							}
							GetAdsResponse.Ad ad = ads.get(0);
							minimalAd = new MinimalAd(getAdsResponse.getAds().get(0));

							if (appViewInstallDisplayable != null) {
								appViewInstallDisplayable.setCpdUrl(ad.getInfo().getCpdUrl());
								appViewInstallDisplayable.setCpiUrl(ad.getInfo().getCpiUrl());
							}
							DataproviderUtils.knock(minimalAd.getCpcUrl());
						});
			}

			if (storeTheme == null) {
				storeTheme = getApp.getNodes().getMeta().getData().getStore().getAppearance().getTheme();
			}

			// useful data for the schedule updates menu option
			GetAppMeta.App app = getApp.getNodes().getMeta().getData();
			scheduled = new Scheduled(app);

			header.setup(getApp);
			setupDisplayables(getApp);
			setupObservables(getApp);
			finishLoading();
		}, refresh);
	}

	@Override
	public int getContentViewId() {
		return VIEW_ID;
	}

	@Override
	public void setupViews() {
		super.setupViews();
		//		this.showAppInfo();

		final AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
		ActionBar supportActionBar = parentActivity.getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
			supportActionBar.setTitle("");
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (storeTheme != null) {
			ThemeUtils.setStatusBarThemeColor(getActivity(), StoreThemeEnum.get("default"));
		}
	}

	@Override
	public void bindViews(View view) {
		super.bindViews(view);
		header = new AppViewHeader(view);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_appview_fragment, menu);
		SearchUtils.setupGlobalSearchView(menu, getActivity());
		uninstallMenuItem = menu.findItem(R.id.menu_uninstall);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int i = item.getItemId();

		if (i == android.R.id.home) {

			getActivity().onBackPressed();
			return true;
		} else if (i == R.id.menu_share) {

			ShowMessage.asSnack(item.getActionView(), "TO DO");
			// TODO: 19/07/16 sithengineer
			return true;
		} else if (i == R.id.menu_schedule) {

			@Cleanup
			Realm realm = Database.get();
			realm.beginTransaction();
			realm.copyToRealmOrUpdate(scheduled);
			realm.commitTransaction();

			String str = this.getString(R.string.added_to_scheduled);
			ShowMessage.asSnack(this.getView(), str);
			return true;
		} else if (i == R.id.menu_uninstall && unInstallAction != null) {
			unInstallAction.call();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//
	// Scrollable interface
	//

	@Override
	public void scroll(Position position) {
		RecyclerView rView = getRecyclerView();
		if (rView == null || getAdapter().getItemCount() == 0) {
			Logger.e(TAG, "Recycler view is null or there are no elements in the adapter");
			return;
		}

		if (position == Position.FIRST) {
			rView.smoothScrollToPosition(0);
		} else if (position == Position.LAST) {
			rView.smoothScrollToPosition(getAdapter().getItemCount());
		}
	}

	@Override
	public void itemAdded(int pos) {
		getLayoutManager().onItemsAdded(getRecyclerView(), pos, 1);
	}

	@Override
	public void itemRemoved(int pos) {
		getLayoutManager().onItemsRemoved(getRecyclerView(), pos, 1);
	}

	@Override
	public void itemChanged(int pos) {
		getLayoutManager().onItemsUpdated(getRecyclerView(), pos, 1);
	}

	//
	// micro widget for header
	//

	@Override
	public void onResume() {
		super.onResume();
		if (memoryArgs.containsKey(BAR_EXPANDED) && header != null && header.getAppBarLayout() != null) {
			boolean isExpanded = memoryArgs.getBoolean(BAR_EXPANDED);
			header.getAppBarLayout().setExpanded(isExpanded);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		if (header != null && header.getAppBarLayout() != null) {
			boolean animationsEnabled = ManagerPreferences.getAnimationsEnabledStatus();
			memoryArgs.putBoolean(BAR_EXPANDED, animationsEnabled ? header.getAppIcon().getAlpha() > 0.9f : header.getAppIcon()
					.getVisibility() == View.VISIBLE);
		}
	}

	@Override
	public void setUnInstallMenuOptionVisible(@Nullable Action0 unInstallAction) {
		this.unInstallAction = unInstallAction;
		uninstallMenuItem.setVisible(unInstallAction != null);
	}

	private enum BundleKeys {
		APP_ID,
		MINIMAL_AD
	}

	private final class AppViewHeader {

		private final boolean animationsEnabled;

		// views
		@Getter private final AppBarLayout appBarLayout;

		@Getter private final CollapsingToolbarLayout collapsingToolbar;

		@Getter private final ImageView featuredGraphic;

		@Getter private final ImageView badge;

		@Getter private final TextView badgeText;

		@Getter private final ImageView appIcon;

		@Getter private final TextView fileSize;

		@Getter private final TextView downloadsCount;

		// ctor
		public AppViewHeader(@NonNull View view) {
			animationsEnabled = ManagerPreferences.getAnimationsEnabledStatus();

			appBarLayout = (AppBarLayout) view.findViewById(R.id.app_bar);
			collapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.collapsing_toolbar);
			appIcon = (ImageView) view.findViewById(R.id.app_icon);
			featuredGraphic = (ImageView) view.findViewById(R.id.featured_graphic);
			badge = (ImageView) view.findViewById(R.id.badge_img);
			badgeText = (TextView) view.findViewById(R.id.badge_text);
			fileSize = (TextView) view.findViewById(R.id.file_size);
			downloadsCount = (TextView) view.findViewById(R.id.downloads_count);
		}

		// setup methods
		public void setup(@NonNull GetApp getApp) {

			GetAppMeta.App app = getApp.getNodes().getMeta().getData();

			String headerImageUrl = app.getGraphic();
			List<GetAppMeta.Media.Screenshot> screenshots = app.getMedia().getScreenshots();

			if (!TextUtils.isEmpty(headerImageUrl)) {
				ImageLoader.load(app.getGraphic(), featuredGraphic);
			}
			else if (screenshots != null && screenshots.size() > 0 && !TextUtils.isEmpty(screenshots.get(0).getUrl())) {
				ImageLoader.load(screenshots.get(0).getUrl(), featuredGraphic);
			}

			if (app.getIcon() != null) {
				ImageLoader.load(getApp.getNodes().getMeta().getData().getIcon(), appIcon);
			}

			collapsingToolbar.setTitle(app.getName());
			StoreThemeEnum storeThemeEnum = StoreThemeEnum.get(storeTheme);
			collapsingToolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), storeThemeEnum.getStoreHeader()));
			collapsingToolbar.setContentScrimColor(ContextCompat.getColor(getActivity(), storeThemeEnum.getStoreHeader()));
			ThemeUtils.setStatusBarThemeColor(getActivity(), StoreThemeEnum.get(storeTheme));
			appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {

				@Override
				public void onStateChanged(AppBarLayout appBarLayout, State state) {
					switch (state) {
						case EXPANDED:
							if (animationsEnabled) {
								appIcon.animate().alpha(1F).start();
							} else {
								appIcon.setVisibility(View.VISIBLE);
							}
							break;

						default:
						case IDLE:
						case COLLAPSED:
							if (animationsEnabled) {
								appIcon.animate().alpha(0F).start();
							} else {
								appIcon.setVisibility(View.INVISIBLE);
							}
							break;
					}
				}
			});

			fileSize.setText(AptoideUtils.StringU.formatBits(AppUtils.sumFileSizes(app.getSize(), app.getObb())));

			downloadsCount.setText(AptoideUtils.StringU.withSuffix(app.getStats().getDownloads()));

			@DrawableRes
			int badgeResId = 0;
			@StringRes
			int badgeMessageId = 0;
			switch (app.getFile().getMalware().getRank()) {
				case TRUSTED:
					badgeResId = R.drawable.ic_badge_trusted;
					badgeMessageId = R.string.appview_header_trusted_text;
					break;

				case WARNING:
					badgeResId = R.drawable.ic_badge_warning;
					badgeMessageId = R.string.warning;
					break;

				default:
				case UNKNOWN:
					badgeResId = R.drawable.ic_badge_unknown;
					badgeMessageId = R.string.unknown;
					break;
			}

			ImageLoader.load(badgeResId, badge);
			badgeText.setText(badgeMessageId);
		}
	}
}

