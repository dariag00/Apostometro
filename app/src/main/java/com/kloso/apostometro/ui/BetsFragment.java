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
import com.kloso.apostometro.model.State;
import com.kloso.apostometro.model.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BetsFragment extends Fragment implements BetsAdapter.BetClickListener {


    @BindView(R.id.rv_bets)
    RecyclerView betsRecyclerView;

    private BetsAdapter adapter;
    private User user;
    private Context activity;
    private final String TAG = this.getClass().getName();
    private List<State> acceptedStates;


    public BetsFragment(User user, Context activity, List<State> acceptedStates){
        this.user = user;
        this.activity = activity;
        this.acceptedStates = acceptedStates;
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
        if(acceptedStates.contains(State.OPEN)){
            return activity.getResources().getString(R.string.open_bets);
        } else if(acceptedStates.contains(State.RESOLVED)){
            return activity.getResources().getString(R.string.pending_payment_bets);
        } else {
            return activity.getResources().getString(R.string.closed_bets);
        }
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
            bets = bets.stream().filter(bet -> acceptedStates.contains(bet.getState())).collect(Collectors.toList());
            //TODO preguntar si mostrar primero las viejas o las nuevas
            Collections.sort(bets, (bet1, bet2) -> bet2.getCreationDate().compareTo(bet1.getCreationDate()));
            adapter.setBetList(bets);
        });
    }

}
