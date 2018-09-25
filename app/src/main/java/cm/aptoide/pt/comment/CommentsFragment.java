package cm.aptoide.pt.comment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.pt.R;
import cm.aptoide.pt.comment.data.Comment;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.fragment.NavigationTrackFragment;
import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

public class CommentsFragment extends NavigationTrackFragment implements CommentsView {

  @Inject CommentsPresenter commentsPresenter;
  @Inject AptoideUtils.DateTimeU dateUtils;
  private RecyclerView commentsList;
  private CommentsAdapter commentsAdapter;
  private SwipeRefreshLayout swipeRefreshLayout;
  private View loading;
  private View genericErrorView;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_comments, container, false);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentComponent(savedInstanceState).inject(this);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    loading = view.findViewById(R.id.progress_bar);
    genericErrorView = view.findViewById(R.id.generic_error);
    swipeRefreshLayout = view.findViewById(R.id.refresh_layout);
    swipeRefreshLayout.setColorSchemeResources(R.color.default_progress_bar_color,
        R.color.default_color, R.color.default_progress_bar_color, R.color.default_color);
    commentsList = view.findViewById(R.id.comments_list);
    commentsList.setLayoutManager(
        new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false));
    commentsAdapter = new CommentsAdapter(dateUtils, Collections.emptyList());
    commentsList.setAdapter(commentsAdapter);

    attachPresenter(commentsPresenter);
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Override public void showComments(List<Comment> comments) {
    commentsAdapter.addComments(comments);
  }

  @Override public void showLoading() {
    commentsList.setVisibility(View.GONE);
    genericErrorView.setVisibility(View.GONE);
    loading.setVisibility(View.VISIBLE);
  }

  @Override public void hideLoading() {
    commentsList.setVisibility(View.VISIBLE);
    loading.setVisibility(View.GONE);
    genericErrorView.setVisibility(View.GONE);
  }

  @Override public void showGeneralError() {
    this.genericErrorView.setVisibility(View.VISIBLE);
    this.commentsList.setVisibility(View.GONE);
    this.loading.setVisibility(View.GONE);
    this.swipeRefreshLayout.setRefreshing(false);
  }

  @Override public void hideRefreshLoading() {
    this.swipeRefreshLayout.setRefreshing(false);
  }

  @Override public Observable<Void> refreshes() {
    return RxSwipeRefreshLayout.refreshes(swipeRefreshLayout);
  }

  @Override public void onDestroyView() {
    commentsList = null;
    commentsAdapter = null;
    swipeRefreshLayout = null;
    genericErrorView = null;
    loading = null;
    super.onDestroyView();
  }
}