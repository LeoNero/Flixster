package com.nerone.flixster;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nerone.flixster.models.Config;
import com.nerone.flixster.models.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.nerone.flixster.MovieListActivity.API_BASE_URL;
import static com.nerone.flixster.MovieListActivity.API_KEY_PARAM;

public class MovieDetailsActivity extends AppCompatActivity {
    public static final String VIDEO_KEY = "videoKey";

    @BindView(R.id.rbVoteAverage) RatingBar rbVoteAverage;
    @BindView(R.id.tvVoteCount) TextView tvVoteCount;
    @BindView(R.id.tvPopularity) TextView tvPopularity;
    @BindView(R.id.tvOverview) TextView tvOverview;
    @BindView(R.id.ivBackdropImage) ImageView ivBackdropImage;

    AsyncHttpClient httpClient;
    Movie movie;
    Config config;

    boolean hasVideo = false;
    boolean fetchingVideos = true;
    String videoKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        httpClient = new AsyncHttpClient();

        movie = Parcels.unwrap(getIntent().getParcelableExtra(Movie.class.getSimpleName()));
        config = Parcels.unwrap(getIntent().getParcelableExtra(Config.class.getSimpleName()));

        Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));

        getSupportActionBar().setTitle("Details: " + movie.getTitle());
        ButterKnife.bind(this);
        setViewsValues();
        getVideosInformation();
    }

    private void setViewsValues() {
        tvVoteCount.setText("Total votes: " + movie.getVoteCount());
        tvOverview.setText(movie.getOverview());

        float voteAverage = movie.getVoteAverage().floatValue();
        rbVoteAverage.setRating(voteAverage = voteAverage > 0 ? voteAverage / 2.0f : voteAverage);

        String popularityText = null;
        double popularity = movie.getPopularity();
        if (popularity > 150) {
            popularityText = "Wow! Very popular movie";
        } else {
            popularityText = "This movie is not popular :(";
        }

        tvPopularity.setText(popularityText);

        setupBackdropImage();
    }

    @OnClick(R.id.ivBackdropImage)
    public void onBackdropImageClick(View view) {
        if (fetchingVideos) {
            Toast.makeText(getApplicationContext(), "Still checking to see if there is any video...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasVideo) {
            Toast.makeText(getApplicationContext(), "No videos to show", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getApplicationContext(), MovieTrailerActivity.class);
        intent.putExtra(VIDEO_KEY, videoKey);

        startActivity(intent);
    }

    private void setupBackdropImage() {
        String imgUrl = config.getImageUrl(config.getBackdropSize(), movie.getBackdropPath());

        int placeholderId = R.drawable.flicks_backdrop_placeholder;

        Glide.with(getApplicationContext())
                .load(imgUrl)
                .bitmapTransform(new RoundedCornersTransformation(getApplicationContext(), 10, 0))
                .placeholder(placeholderId)
                .error(placeholderId)
                .into(ivBackdropImage);
    }

    private void getVideosInformation() {
        final String movieVideosUrl = API_BASE_URL + "/movie/" + movie.getId() + "/videos";

        RequestParams movieVideosParams = new RequestParams();
        movieVideosParams.put(API_KEY_PARAM, getString(R.string.api_key));

        httpClient.get(movieVideosUrl, movieVideosParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                fetchingVideos = false;

                try {
                    JSONArray results = response.getJSONArray("results");

                    if (results.length() == 0) {
                        hasVideo = false;
                    } else {
                        hasVideo = true;
                        videoKey = results.getJSONObject(0).getString("key");
                    }
                } catch (JSONException e) {
                    String message = "Failure to parse data from videos endpoint";
                    Log.e("MovieDetailsActivity", message, e);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                hasVideo = false;
                fetchingVideos = false;

                String message = "Failure to get data from videos endpoint";
                Log.e("MovieDetailsActivity", message, throwable);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
