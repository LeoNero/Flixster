package com.nerone.flixster;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nerone.flixster.models.Config;
import com.nerone.flixster.models.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

public class MovieListActivity extends AppCompatActivity {
    public static final String API_BASE_URL = "https://api.themoviedb.org/3";
    public static final String API_KEY_PARAM = "api_key";
    public static final String LOG_TAG = "MovieListActivity";

    @BindView(R.id.rvMovies) RecyclerView rvMovies;

    List<Movie> movies;
    AsyncHttpClient httpClient;
    MovieAdapter movieAdapter;
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        ButterKnife.bind(this);

        movies = new ArrayList<>();
        httpClient = new AsyncHttpClient();

        movieAdapter = new MovieAdapter(movies);

        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(movieAdapter);

        getConfigurationFromApi();
    }

    private void getConfigurationFromApi() {
        String configurationUrl = API_BASE_URL + "/configuration";

        RequestParams configurationParams = new RequestParams();
        configurationParams.put(API_KEY_PARAM, getString(R.string.api_key));

        httpClient.get(configurationUrl, configurationParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    config = new Config(response);

                    Log.i(
                            LOG_TAG,
                            String.format(
                                    "Loaded configuration with imageBaseUrl %s and posterSize %s",
                                    config.getImageBaseUrl(),
                                    config.getPosterSize()
                            )
                    );

                    movieAdapter.setConfig(config);

                    getNowPlayingMovies();
                } catch (JSONException e) {
                    logError("Failed parsing configuration", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed getting configuration", throwable, true);
            }
        });
    }

    private void getNowPlayingMovies() {
        String nowPlayingUrl = API_BASE_URL + "/movie/now_playing";

        RequestParams nowPlayingParams = new RequestParams();
        nowPlayingParams.put(API_KEY_PARAM, getString(R.string.api_key));

        httpClient.get(nowPlayingUrl, nowPlayingParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);
                        movieAdapter.notifyItemInserted(movies.size() - 1);
                    }

                    Log.i(LOG_TAG, String.format("Loaded %s movies", results.length()));
                } catch (JSONException e) {
                    logError("Failed to parse now playing movies", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failure to get data from now_playing endpoint", throwable, true);
            }
        });
    }

    private void logError(String message, Throwable error, boolean alertUser) {
        Log.e(LOG_TAG, message, error);

        if (alertUser) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
