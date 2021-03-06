package org.fossasia.openevent.activities;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.SessionsListAdapter;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.dbutils.RealmDataRepository;
import org.fossasia.openevent.utils.ConstantStrings;
import org.fossasia.openevent.utils.DateConverter;
import org.fossasia.openevent.utils.Utils;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * User: MananWason
 * Date: 8/18/2015
 */
public class LocationActivity extends BaseActivity implements SearchView.OnQueryTextListener {
    final private String SEARCH = "searchText";

    private SessionsListAdapter sessionsListAdapter;

    private final String FRAGMENT_TAG_LOCATION = "FTAGR";

    private GridLayoutManager gridLayoutManager;
    private Dialog upcomingDialogBox;

    private List<Session> sessions = new ArrayList<>();

    private static final int locationWiseSessionList = 1;

    @BindView(R.id.recyclerView_locations)
    RecyclerView sessionRecyclerView;
    @BindView(R.id.txt_no_sessions)
    TextView noSessionsView;
    @BindView(R.id.toolbar_locations)
    Toolbar toolbar;

    private ImageView trackImageIcon;

    private String location;

    private String searchText = "";

    private SearchView searchView;
    private Menu menu;

    private RealmDataRepository realmRepo = RealmDataRepository.getDefaultInstance();
    private TextView upcomingSessionText;
    private TextView upcomingSessionTitle;
    private TextDrawable.IBuilder drawableBuilder = TextDrawable.builder().round();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setUpcomingSessionsDialog();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState != null && savedInstanceState.getString(SEARCH) != null) {
            searchText = savedInstanceState.getString(SEARCH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        location = getIntent().getStringExtra(ConstantStrings.LOCATION_NAME);
        toolbar.setTitle(location);

        //setting the grid layout to cut-off white space in tablet view
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);

        sessionRecyclerView.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(this, spanCount);
        sessionRecyclerView.setLayoutManager(gridLayoutManager);
        sessionsListAdapter = new SessionsListAdapter(this, sessions, locationWiseSessionList);
        sessionRecyclerView.setAdapter(sessionsListAdapter);
        sessionRecyclerView.scrollToPosition(SessionsListAdapter.listPosition);
        sessionRecyclerView.setItemAnimator(new DefaultItemAnimator());

        loadData();

        handleVisibility();
    }

    private void loadData() {
        realmRepo.getSessionsByLocation(location)
                .addChangeListener((sessions, orderedCollectionChangeSet) -> {
                    this.sessions.clear();
                    this.sessions.addAll(sessions);

                    setUpcomingSession();
                    sessionsListAdapter.setCopyOfSessions(sessions);
                    sessionsListAdapter.notifyDataSetChanged();
                    if (!Utils.isEmpty(searchText))
                        sessionsListAdapter.filter(searchText);
                    handleVisibility();
                });
    }

    public void setUpcomingSessionsDialog() {
        upcomingDialogBox = new Dialog(this);
        upcomingDialogBox.setContentView(R.layout.upcoming_dialogbox);
        trackImageIcon = (ImageView) upcomingDialogBox.findViewById(R.id.track_image_drawable);
        upcomingSessionText = (TextView) upcomingDialogBox.findViewById(R.id.upcoming_session_textview);
        upcomingSessionTitle = (TextView) upcomingDialogBox.findViewById(R.id.upcoming_Session_title);
        Button dialogButton = (Button) upcomingDialogBox.findViewById(R.id.upcoming_button);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upcomingDialogBox.dismiss();
            }
        });
    }

    public void setUpcomingSession() {
        String upcomingTitle = "";
        String track = "";
        String color = null;
        ZonedDateTime current = ZonedDateTime.now();
        for (Session session : sessions) {
            ZonedDateTime start = DateConverter.getDate(session.getStartsAt());
            if (start.isAfter(current)) {
                upcomingTitle = session.getTitle();
                track = session.getTrack().getName();
                color = session.getTrack().getColor();
                break;
            }
        }

        if (!TextUtils.isEmpty(upcomingTitle)) {
            int trackColor = Color.parseColor(color);
            upcomingSessionTitle.setText(getResources().getString(R.string.upcoming_sess));
            TextDrawable drawable = drawableBuilder.build(String.valueOf(track.charAt(0)), trackColor);
            trackImageIcon.setImageDrawable(drawable);
            trackImageIcon.setBackgroundColor(Color.TRANSPARENT);
            upcomingSessionText.setText(upcomingTitle);
        } else {
            upcomingSessionTitle.setText(getResources().getString(R.string.no_upcoming_Sess));
            upcomingSessionText.setVisibility(View.GONE);
        }
    }

    private void handleVisibility() {
        if (!sessions.isEmpty()) {
            noSessionsView.setVisibility(View.GONE);
            sessionRecyclerView.setVisibility(View.VISIBLE);
        } else {
            noSessionsView.setVisibility(View.VISIBLE);
            sessionRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_locations;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (searchView != null) {
            bundle.putString(SEARCH, searchText);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_location_activity, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search_tracks_location).getActionView();
        DrawableCompat.setTint(menu.findItem(R.id.action_search_tracks_location).getIcon(), Color.WHITE);
        DrawableCompat.setTint(menu.findItem(R.id.action_map_location).getIcon(), Color.WHITE);
        searchView.setOnQueryTextListener(this);
        searchView.setQuery(searchText, false);
        return true;
    }

    @Override
    public void onBackPressed() {
        if ((sessions.isEmpty())) {
            noSessionsView.setVisibility(View.VISIBLE);
        } else {
            sessionRecyclerView.setVisibility(View.VISIBLE);
        }
        menu.setGroupVisible(R.id.menu_group_location_activity, true);
        super.onBackPressed();

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map_location:
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                Bundle bundle = new Bundle();
                bundle.putBoolean(ConstantStrings.IS_MAP_FRAGMENT_FROM_MAIN_ACTIVITY, false);
                bundle.putString(ConstantStrings.LOCATION_NAME, location);

                Fragment mapFragment = ((OpenEventApp) getApplication())
                        .getMapModuleFactory()
                        .provideMapModule()
                        .provideMapFragment();
                mapFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.content_frame_location, mapFragment, FRAGMENT_TAG_LOCATION).addToBackStack(null).commit();

                sessionRecyclerView.setVisibility(View.GONE);
                noSessionsView.setVisibility(View.GONE);
                menu.setGroupVisible(R.id.menu_group_location_activity, false);
                return true;
            case android.R.id.home:
                onBackPressed();
                getSupportFragmentManager().popBackStack();
                sessionRecyclerView.setVisibility(View.VISIBLE);
                return true;
            case R.id.upcoming_sessions:
                upcomingDialogBox.show();
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String query) {
        searchText = query;
        sessionsListAdapter.filter(searchText);

        return false;
    }
}