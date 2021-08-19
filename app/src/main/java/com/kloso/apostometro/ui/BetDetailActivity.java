package com.kloso.apostometro.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.toolbox.Volley;
import com.kloso.apostometro.BetRepository;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.NotificationUtils;
import com.kloso.apostometro.R;
import com.kloso.apostometro.UsersAdapter;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.State;
import com.kloso.apostometro.model.Result;
import com.kloso.apostometro.model.User;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BetDetailActivity extends AppCompatActivity {


    @BindView(R.id.tv_title)
    TextView titleView;
    @BindView(R.id.tv_rewards)
    TextView rewardsView;
    @BindView(R.id.rv_users_in_favour)
    RecyclerView favourRecyclerView;
    @BindView(R.id.rv_users_against)
    RecyclerView againstRecyclerView;
    @BindView(R.id.button_mark_resolved)
    Button markAsResolvedButton;
    @BindView(R.id.button_mark_paid)
    Button markAsPaidButton;
    @BindView(R.id.tv_state)
    TextView stateView;
    @BindView(R.id.tv_result)
    TextView resultView;
    @BindView(R.id.tv_limit_date)
    TextView limitDateView;
    @BindView(R.id.tv_created_at)
    TextView createdAtView;
    @BindView(R.id.created_by)
    TextView createdByView;

    private Bet bet;
    private UsersAdapter favourAdapter;
    private UsersAdapter againstAdapter;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bet_detail);

        ButterKnife.bind(this);

        favourAdapter = new UsersAdapter();
        againstAdapter = new UsersAdapter();

        setUpBet();

        setUpRecyclerView(favourRecyclerView, favourAdapter);
        setUpRecyclerView(againstRecyclerView, againstAdapter);
        markAsResolvedButton.setOnClickListener(view -> showConfirmationDialogMarkResolved());
        if(bet.getState() == State.RESOLVED) {
            markAsPaidButton.setVisibility(View.VISIBLE);
            markAsPaidButton.setOnClickListener(view -> showConfirmationDialogMarkPaid());
        } else {
            markAsPaidButton.setVisibility(View.GONE);
        }

    }

    private void setUpBet(){
        Intent previousIntent = getIntent();
        String betId = null;
        if(previousIntent.hasExtra(Constants.BET)) {
            bet = (Bet) previousIntent.getSerializableExtra(Constants.BET);
            betId = bet.getId();
            setBetData();
        } else if(previousIntent.hasExtra(Constants.BET_ID)){
            betId = previousIntent.getStringExtra(Constants.BET_ID);
        }

        FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
        firestoreViewModel.getBetLiveData(betId).observe(this, liveDataBet -> {
            bet = liveDataBet;
            if(bet != null) {
                setBetData();
            }
        });
        firestoreViewModel.getUserByEmail(new BetRepository().getFirebaseUser().getEmail()).observe(this, user -> {
            this.user = user;
        });
    }

    private void showConfirmationDialogMarkResolved() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getResources().getString(R.string.mark_resolved));
        alert.setMessage(this.getResources().getString(R.string.who_won));
        alert.setPositiveButton(R.string.favour_s, (dialog, which) -> {
            updateBet(State.RESOLVED, Result.WON_BY_FAVOUR);
            dialog.cancel();
            finish();
        });
        alert.setNegativeButton(R.string.against_s, (dialog, which) -> {
            updateBet(State.RESOLVED, Result.WON_BY_AGAINST);
            dialog.cancel();
            finish();
        });

        alert.setOnCancelListener(DialogInterface::cancel);
        alert.show();
    }

    private void showConfirmationDialogMarkPaid() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getResources().getString(R.string.mark_paid));
        alert.setMessage(this.getResources().getString(R.string.paid_confirmation));
        alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            updateBet(State.PAID, null);
            dialog.cancel();
            finish();
        });
        alert.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            finish();
        });

        alert.setOnCancelListener(DialogInterface::cancel);
        alert.show();
    }

    private void updateBet(State betState, Result result){
        bet.setState(betState);
        if(result != null){
            bet.setResult(result);
        }
        FirestoreViewModel viewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
        viewModel.updateBet(bet);
        setBetData();

        Set<String> tokens = bet.getParticipantTokens();
        tokens = tokens.stream().filter(token -> !token.equals(user.getTokenId())).collect(Collectors.toSet());

        NotificationUtils notificationUtils = new NotificationUtils(this, Volley.newRequestQueue(this));
        String title;
        String message;
        if(betState.equals(State.RESOLVED)) {
            title = this.getResources().getString(R.string.bet_resolved);
            message = "Se ha resuelto la apuesta \"" + bet.getTitle()+ "\". A pagar!";
        } else {
            title = this.getResources().getString(R.string.bet_paid);
            message = "Se ha marcado la apuesta \"" + bet.getTitle()+ "\" como pagada. Disfruten signiores.";
        }

        for(String token : tokens) {
            notificationUtils.notifyUsers(token, title, message);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int clickedItem = item.getItemId();
        if(clickedItem == R.id.action_delete){
            showConfirmationDialogDeleteBet();
            return true;
        } else if(clickedItem == R.id.action_edit){
            Intent intent = new Intent(this, CreateBetActivity.class);
            intent.putExtra(Constants.BET, bet);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConfirmationDialogDeleteBet() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getResources().getString(R.string.delete_bet));
        alert.setMessage(this.getResources().getString(R.string.delete_bet_confirm_1));
        alert.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            delete();
        });
        alert.setNegativeButton(android.R.string.no, (dialog, which) -> {
            dialog.cancel();
        });
        alert.show();
    }

    private void delete(){
        new BetRepository().deleteBet(bet);
        finish();
    }

    private void setUpRecyclerView(RecyclerView recyclerView, UsersAdapter usersAdapter){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(usersAdapter);
    }

    private void setBetData(){
        titleView.setText(bet.getTitle());
        rewardsView.setText(bet.getReward());
        stateView.setText(this.getResources().getString(Integer.valueOf(bet.getState().toString())));
        resultView.setText(this.getResources().getString(Integer.valueOf(bet.getResult().toString())));
        createdByView.setText(bet.getCreatedBy().getFullName());
        if(bet.getDueDate() != null) {
            limitDateView.setText(new SimpleDateFormat("dd/MM/yyyy").format(bet.getDueDate()));
        }
        if(bet.getCreationDate() != null) {
            createdAtView.setText(new SimpleDateFormat("dd/MM/yyyy").format(bet.getCreationDate()));
        }
        favourAdapter.setParticipantList(bet.getUsersInFavour());
        againstAdapter.setParticipantList(bet.getUsersAgainst());
    }

}
