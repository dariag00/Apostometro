package com.kloso.apostometro.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.kloso.apostometro.BetRepository;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.FragmentStatePagerAdapter;
import com.kloso.apostometro.R;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.User;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getName();


    @BindView(R.id.fb_main_menu)
    FloatingActionButton mainFab;
    @BindView(R.id.tv_bets_won)
    TextView wonBetsView;
    @BindView(R.id.tv_bets_lost)
    TextView lostBetsView;
    @BindView(R.id.tv_pending_bets)
    TextView pendingBetsView;
    @BindView(R.id.detail_viewpager)
    ViewPager viewPager;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Intent previousIntent = getIntent();
        user = (User) previousIntent.getSerializableExtra(Constants.USER);

        FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);

        if(user == null){
            firestoreViewModel.getUserByEmail(new BetRepository().getFirebaseUser().getEmail()).observe(this, user -> {
                this.user = user;
                firestoreViewModel.getBets().observe(this, bets -> {
                    Log.i(TAG, "Received new Bets data. Size: " + bets.size());
                    populateCounters(bets);
                });
            });
        } else {
            firestoreViewModel.getBets().observe(this, bets -> {
                Log.i(TAG, "Received new Bets data. Size: " + bets.size());
                populateCounters(bets);
            });
        }

        mainFab.setOnClickListener(fab -> {
            Intent intent = new Intent(this, CreateBetActivity.class);
            startActivity(intent);
        });

        setUpViewPager();

    }

    private void setUpViewPager(){
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new BetsFragment(user, true, this));
        fragmentList.add(new BetsFragment(user, false, this));
        FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, fragmentList);
        viewPager.setAdapter(pagerAdapter);
    }

    private void populateCounters(List<Bet> bets){
        int wonBets = 0, lostBets = 0, pendingBets = 0;

        for(Bet bet : bets){
            if(bet.isPending()){
                pendingBets += 1;
            } else if(bet.haveUserWon(user.getFullName())){
                wonBets += 1;
            } else {
                lostBets += 1;
            }
        }

        wonBetsView.setText(String.valueOf(wonBets));
        lostBetsView.setText(String.valueOf(lostBets));
        pendingBetsView.setText(String.valueOf(pendingBets));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int clickedItem = item.getItemId();
        if(clickedItem == R.id.action_log_out){
            logOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
