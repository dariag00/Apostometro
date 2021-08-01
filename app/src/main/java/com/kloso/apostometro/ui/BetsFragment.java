package com.kloso.apostometro.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kloso.apostometro.BetRepository;
import com.kloso.apostometro.BetsAdapter;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.R;
import com.kloso.apostometro.SwipeToDeleteCallback;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.User;
import com.kloso.apostometro.ui.BetDetailActivity;

import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BetsFragment extends Fragment implements BetsAdapter.BetClickListener {


    @BindView(R.id.rv_bets)
    RecyclerView betsRecyclerView;

    private BetsAdapter adapter;
    private User user;
    private boolean containsOpenBets;
    private Context activity;
    private final String TAG = this.getClass().getName();


    public BetsFragment(User user, boolean containsOpenBets, Context activity){
        this.user = user;
        this.containsOpenBets = containsOpenBets;
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.bets_fragment, container, false);
        ButterKnife.bind(this, rootView);

        if(user == null){
            FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
            firestoreViewModel.getUserByEmail(new BetRepository().getFirebaseUser().getEmail()).observe(this, user -> {
                this.user = user;
                setUpRecyclerView();
            });
        } else {
            setUpRecyclerView();
        }

        return rootView;
    }

    @Override
    public void onBetClick(Bet clickedBet) {
        Intent intent = new Intent(this.getContext(), BetDetailActivity.class);
        intent.putExtra(Constants.BET, clickedBet);
        startActivity(intent);
    }

    public String getTabName(){
        return containsOpenBets ? activity.getResources().getString(R.string.open_bets)
                : activity.getResources().getString(R.string.closed_bets);
    }


    private void setUpRecyclerView(){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getContext());
        betsRecyclerView.setLayoutManager(linearLayoutManager);
        betsRecyclerView.setHasFixedSize(true);
        adapter = new BetsAdapter(this.getActivity(), this, user.getFullName());
        betsRecyclerView.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(this.getContext(), adapter));
        itemTouchHelper.attachToRecyclerView(betsRecyclerView);

        FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
        firestoreViewModel.getBets().observe(this, bets -> {
            Log.i(TAG, "Received new Bets data. Size: " + bets.size());
            bets = bets.stream().filter(bet -> containsOpenBets == bet.isPending()).collect(Collectors.toList());
            adapter.setBetList(bets);
        });
    }

}
