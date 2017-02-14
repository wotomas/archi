package uk.ivanc.archimvp.presenter;

import android.util.Log;

import java.util.List;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import uk.ivanc.archimvp.R;
import uk.ivanc.archimvp.model.GithubService;
import uk.ivanc.archimvp.model.Repository;
import uk.ivanc.archimvp.view.MainMvpView;

public class MainPresenter implements Presenter<MainMvpView> {

    public static String TAG = "MainPresenter";

    private MainMvpView mainMvpView;
    //private Subscription subscription;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private List<Repository> repositories;
    private GithubService service;

    public MainPresenter(GithubService service) {
      this.service = service;
    }

  @Override
    public void attachView(MainMvpView view) {
        this.mainMvpView = view;

        subscriptions.add(
          view.onSearchButtonClicked()
              .doOnNext(v -> mainMvpView.showProgressIndicator())
              .observeOn(Schedulers.io())
              .flatMap(v -> service.publicRepositories(mainMvpView.getUsername()))
              .observeOn(AndroidSchedulers.mainThread())
              //.subscribeOn(application.defaultSubscribeScheduler)  // attach view is always called from UI thread, no need to specify
              .subscribe(new Subscriber<List<Repository>>() {
                @Override
                public void onCompleted() {
                  Log.i(TAG, "Repos onComplete loaded " + repositories);
                }

                @Override
                public void onError(Throwable error) {
                  Log.e(TAG, "Error loading GitHub repos ", error);
                  if (isHttp404(error)) {
                    mainMvpView.showMessage(R.string.error_username_not_found);
                  } else {
                    mainMvpView.showMessage(R.string.error_loading_repos);
                  }
                }

                @Override
                public void onNext(List<Repository> repositories) {
                  Log.i(TAG, "Repos onNext loaded " + repositories);
                  MainPresenter.this.repositories = repositories;
                  if (!repositories.isEmpty()) {
                    mainMvpView.showRepositories(repositories);
                  } else {
                    mainMvpView.showMessage(R.string.text_empty_repos);
                  }
                }
              })
        );
    }

    @Override
    public void detachView() {
        this.mainMvpView = null;
        subscriptions.unsubscribe();
    }

    private static boolean isHttp404(Throwable error) {
        return error instanceof HttpException && ((HttpException) error).code() == 404;
    }

}
