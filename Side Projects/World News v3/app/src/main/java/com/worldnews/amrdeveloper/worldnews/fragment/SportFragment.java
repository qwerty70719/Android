package com.worldnews.amrdeveloper.worldnews.fragment;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.worldnews.amrdeveloper.worldnews.activities.WebViewerActivity;
import com.worldnews.amrdeveloper.worldnews.adapter.NewsCursorAdapter;
import com.worldnews.amrdeveloper.worldnews.adapter.NewsListAdapter;
import com.worldnews.amrdeveloper.worldnews.data.NewsLoaderManager;
import com.worldnews.amrdeveloper.worldnews.loaders.NewsAsyncLoader;
import com.worldnews.amrdeveloper.worldnews.model.News;
import com.worldnews.amrdeveloper.worldnews.R;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by AmrDeveloper on 1/29/2018.
 */

public class SportFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<News>>,
        SwipeRefreshLayout.OnRefreshListener {

    private ListView newsListView;
    private TextView errorMessage;
    private ProgressBar loadingBar;

    //SearchView Menu
    private SearchView searchView = null;
    private SearchView.OnQueryTextListener queryTextListener;

    /**
     * Adapter For List of news
     */
    private NewsListAdapter newsAdapter;

    //Loader Final Id
    private final int LOADER_ID = 3;

    private NetworkInfo networkInfo;

    private SwipeRefreshLayout swipeRefreshLayout;

    private LoaderManager.LoaderCallbacks<List<News>> loaderCallbacksObject = this;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.news_listview, container, false);

        errorMessage = rootView.findViewById(R.id.errorMessage);
        loadingBar = rootView.findViewById(R.id.loadingBar);

        newsAdapter = new NewsListAdapter(getActivity());

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */

        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);
                                        getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacksObject);
                                    }
                                }
        );

        newsListView = rootView.findViewById(R.id.newsListView);
        //Set Empty View to show error message when no data
        newsListView.setEmptyView(errorMessage);
        newsListView.setAdapter(newsAdapter);

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();
            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter.
            loaderManager.initLoader(LOADER_ID, null, this);
        } else {
            // Update empty state with no connection error message
            errorMessage.setText(R.string.no_connection);
            //Get data from database
            NewsCursorAdapter newsCursorAdapter = new NewsCursorAdapter(getContext(), null);
            newsListView.setAdapter(newsCursorAdapter);
            NewsLoaderManager dataLoader = new NewsLoaderManager(getContext(), Api.SECTION_SPORT_DATA, newsCursorAdapter);
            getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, dataLoader);
        }

        return rootView;
    }


    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        //Show Loading Bar and hide ListView and Error TextView
        loadingBar.setVisibility(View.VISIBLE);
        errorMessage.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        //Get Country value from Shared Preferences
        String country = sharedPrefs.getString(
                getString(R.string.news_country_key),
                getString(R.string.news_country_default)
        );

        //Get Order By value from Shared Preferences
        String orderBy = sharedPrefs.getString(
                getString(R.string.news_order_key),
                getString(R.string.news_order_default)
        );

        Uri baseUri = Uri.parse(Api.API_LINK);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        //Append Attribute to The Link
        uriBuilder.appendQueryParameter(Api.QUERY, country);
        uriBuilder.appendQueryParameter(Api.ORDER_BY, orderBy);
        uriBuilder.appendQueryParameter(Api.SECTION, Api.SECTION_SPORT);
        uriBuilder.appendQueryParameter(Api.SHOW_TAGS, Api.CONTRIBUTOR);
        uriBuilder.appendQueryParameter(Api.SHOW_FIELDS, Api.API_FIELDS_INPUT);
        uriBuilder.appendQueryParameter(Api.PAGE_SIZE, Api.PAGE_SIZE_NUM);
        uriBuilder.appendQueryParameter(Api.API_KEY, Api.MY_API_KEY);

        //Start Loader
        return new NewsAsyncLoader(
                getActivity()
                , uriBuilder.toString(),
                Api.SECTION_SPORT_DATA);
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> data) {
        ///Hide the indicator after the data is appeared
        loadingBar.setVisibility(View.GONE);

        // Check if connection is still available, otherwise show appropriate message
        if (networkInfo != null && networkInfo.isConnected()) {
            // If there is a valid list of news stories, then add them to the adapter's
            // data set. This will trigger the ListView to update.
            if (data != null && !data.isEmpty()) {
                newsAdapter.clear();
                newsAdapter.addAll(data);
            } else {
                errorMessage.setVisibility(View.VISIBLE);
                errorMessage.setText(getString(R.string.no_data));
            }
        } else {
            errorMessage.setText(R.string.no_connection);
        }
        swipeRefreshLayout.setRefreshing(false);
    }
    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        //Clear The Data From ListView
        newsAdapter.clear();
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        getLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacksObject);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();

        inflater.inflate(R.menu.search_menu, menu);
        inflater.inflate(R.menu.settings_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setQueryHint("Search for News");
            queryTextListener = new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    newsAdapter.getFilter().filter(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    newsAdapter.getFilter().filter(query);
                    if(newsAdapter.getCount() == 0){
                        messageDialog();
                    }

                    return true;
                }
            };
            searchView.setOnQueryTextListener(queryTextListener);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void messageDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        builder.setMessage("No Match. Please try again");

        // Create and show the AlertDialog
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                alertDialog.dismiss();
                timer.cancel(); // This will cancel the timer of the system
            }
        }, 2000); // the timer will count 2 seconds....
    }
}
