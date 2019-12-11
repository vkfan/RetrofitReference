package com.radio.chat.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.radio.chat.R;
import com.radio.chat.activities.AddPostActivity;
import com.radio.chat.activities.Change_profilePic;
import com.radio.chat.activities.DashboardActivity;
import com.radio.chat.activities.UserPostActivity;
import com.radio.chat.adapters.HomeAdapter;
import com.radio.chat.data.RadioPreferences;
import com.radio.chat.interfaces.CallPost;
import com.radio.chat.interfaces.PostUpdates;
import com.radio.chat.models.PostModel;
import com.radio.chat.reciever.ConnectivityReceiver;
import com.radio.chat.rest.APIExecutor;
import com.radio.chat.utils.AppUtils;
import com.radio.chat.utils.PaginationScrollListener;
import com.tylersuehr.esr.EmptyStateRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Response;

public class HomeFragment extends Fragment implements CallPost, PostUpdates {


    private static int PAGE_START = 1;
    @BindView(R.id.posts_recyclerview)
    EmptyStateRecyclerView mPostsRecyclerView;
    @BindView(R.id.addnew_post)
    FloatingActionButton addnew_post;
    HomeAdapter adapter;
    LinearLayoutManager linearLayoutManager;
    AppUtils utils;
    RadioPreferences rp;
    @BindView(R.id.main_progress)
    LottieAnimationView progressBar;
    @BindView(R.id.postprogresslay)
    RelativeLayout postprogresslay;
    @BindView(R.id.progressBar)
    ProgressBar postprogressBar;
    @BindView(R.id.no_internet)
    LottieAnimationView no_internet;
    private List<PostModel> posts;
    private Gson gson;
    private boolean isViewShown = false;
    private int current_page = 1, last_page = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 15;
    private int currentPage = PAGE_START;


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null) {
            isViewShown = true;
            Log.e("home", "true");
        } else {
            isViewShown = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment_activity, container, false);
        ButterKnife.bind(this, view);
        DashboardActivity.setCallPost(this);
        AddPostActivity.setCallPost(this);
        AddPostActivity.setPostUpdates(this);
        UserPostActivity.setCallPost(this);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rp = RadioPreferences.getInstance(getContext());
        utils = new AppUtils(getActivity());
        posts = new ArrayList<>();
        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mPostsRecyclerView.setLayoutManager(linearLayoutManager);
        adapter = new HomeAdapter(getActivity());
        mPostsRecyclerView.setAdapter(adapter);

        mPostsRecyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager, addnew_post) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getPosts(current_page + 1);
                    }
                }, 1000);
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });

        callFirstPage();
    }


    private void loadFirstPage(List<PostModel> posts) {
        Log.d("Home", "loadFirstPage: ");
        progressBar.setVisibility(View.GONE);

        adapter.addAll(posts);
        if (currentPage <= last_page) {
            adapter.addLoadingFooter();
        } else {
            isLastPage = true;
        }
        adapter.removeLoader();

    }

    private void loadNextPage(List<PostModel> posts) {
        Log.d("Home", "loadNextPage: " + currentPage);
        adapter.removeLoadingFooter();
        isLoading = false;
        adapter.addAll(posts);
        Log.e("TOTAL_PAGES", String.valueOf(TOTAL_PAGES));
        if (currentPage <= last_page)
            adapter.addLoadingFooter();
        else
            isLastPage = true;
        adapter.removeLoader();
    }


    @OnClick(R.id.addnew_post)
    public void setAddnew_post() {
        openActivity(addnew_post);
    }


    private void getPosts(int page) {
        try {
            isLoading = true;
            Log.e("tocken", rp.getUserToken(""));
            Log.e("page", "" + page);
            Call<JsonObject> call = APIExecutor.getApiService().getPosts(rp.getUserToken(""), page);
            utils.writeErrorLog("url " + call.request().url());

            call.enqueue(new retrofit2.Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    isLoading = false;
                    utils.writeErrorLog("posts response" + new Gson().toJson(response.body()));
                    if (response.body() != null) {

                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.setDateFormat("M/d/yy hh:mm a");
                        gson = gsonBuilder.create();
                        if (response.body().get("status").getAsString().equalsIgnoreCase("success")) {
                            //  if (posts.size() > 0)
                            //     posts.addAll(new ArrayList<>(Arrays.asList(gson.fromJson(response.body().getAsJsonArray("data"), PostModel[].class))));
                            //  else

                            JsonObject meta = response.body().getAsJsonObject("meta");
                            current_page = meta.get("current_page").getAsInt();
                            last_page = meta.get("last_page").getAsInt();
                            if (current_page == last_page) {
                                isLastPage = true;
                                ///   adapter.removeLoadingFooter();
                            } else {
                                isLastPage = false;
                            }
                            posts = new ArrayList<>(Arrays.asList(gson.fromJson(response.body().getAsJsonArray("data"), PostModel[].class)));
                            utils.writeErrorLog("posts" + posts.size());
                            if (posts.size() > 0) {
                                if (currentPage == 1) {
                                    loadFirstPage(posts);
                                    Log.e("loaging first page_!W!W", "yes");
                                } else {
                                    Log.e("loaging next page .....", "yes");
                                    loadNextPage(posts);
                                }
                                mPostsRecyclerView.invokeState(EmptyStateRecyclerView.STATE_OK);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                onLoadingFailed();
                            }


                        } else {
                            onLoadingFailed();
                            utils.showShortToast("Connectivity issue", AppUtils.WarningToast);
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    isLoading = false;
                    utils.showShortToast("Failure", AppUtils.ErrorToast);
                    t.printStackTrace();
                    utils.writeErrorLog(t.getMessage());
                    onLoadingFailed();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onLoadingFailed() {
        mPostsRecyclerView.invokeState(EmptyStateRecyclerView.STATE_EMPTY);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void callFirstPage() {
        PAGE_START = 1;
        current_page = 1;
        last_page = 0;
        isLoading = false;
        isLastPage = false;
        TOTAL_PAGES = 15;
        currentPage = PAGE_START;
        adapter = new HomeAdapter(getActivity());
        mPostsRecyclerView.setAdapter(adapter);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (checkConnection()) {
                    mPostsRecyclerView.invokeState(EmptyStateRecyclerView.STATE_LOADING);
                    no_internet.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                 getPosts(currentPage);
                } else {
                    no_internet.setVisibility(View.VISIBLE);
                    no_internet.setSpeed(1);
                    progressBar.setVisibility(View.GONE);
                }

            }
        }, 1000);
    }

    public void openActivity(View view) {
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(getActivity(), view, "transition");
        int revealX = (int) (view.getX() + view.getWidth() / 2);
        int revealY = (int) (view.getY() + view.getHeight() / 2);

        Intent intent = new Intent(getContext(), AddPostActivity.class);
        intent.putExtra(Change_profilePic.EXTRA_CIRCULAR_REVEAL_X, revealX);
        intent.putExtra(Change_profilePic.EXTRA_CIRCULAR_REVEAL_Y, revealY);

        ActivityCompat.startActivity(getContext(), intent, options.toBundle());
    }

    public boolean checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        return isConnected;
    }


    @Override
    public void onPostCall() {
        callFirstPage();
        postprogresslay.setVisibility(View.GONE);
    }


    @Override
    public void onPostCall(int update) {
        try {
            postprogresslay.setVisibility(View.VISIBLE);
            if (rp.getUploading("").equals("yes")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    postprogressBar.setProgress(update, true);
                }
            } else {
                postprogresslay.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
