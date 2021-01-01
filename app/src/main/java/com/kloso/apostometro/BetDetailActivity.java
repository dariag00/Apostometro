package com.kloso.apostometro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.Participant;
import com.kloso.apostometro.model.User;
import com.kloso.apostometro.ui.CreateBetActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @BindView(R.id.tv_state)
    TextView stateView;
    @BindView(R.id.tv_limit_date)
    TextView limitDateView;
    @BindView(R.id.tv_created_at)
    TextView createdAtView;
    @BindView(R.id.created_by)
    TextView createdByView;

    private Bet bet;
    private UsersAdapter favourAdapter;
    private UsersAdapter againstAdapter;
    private String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private RequestQueue requestQueue;

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

        requestQueue = Volley.newRequestQueue(this);
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
    }

    private void showConfirmationDialogMarkResolved() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getResources().getString(R.string.mark_resolved));
        alert.setMessage(this.getResources().getString(R.string.who_won));
        alert.setPositiveButton(R.string.favour_s, (dialog, which) -> {
            updateBet(true);
            dialog.cancel();
            finish();
        });
        alert.setNegativeButton(R.string.against_s, (dialog, which) -> {
            updateBet(false);
            dialog.cancel();
            finish();
        });

        alert.setOnCancelListener(DialogInterface::cancel);
        alert.show();
    }

    private void updateBet(boolean isFavour){
        if(isFavour){
            bet.setWon(true);
        } else {
            bet.setWon(false);
        }
        bet.setPending(false);
        FirestoreViewModel viewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
        viewModel.updateBet(bet);
        setBetData();

        Set<String> tokens = new HashSet<>();

        for(Participant participant : bet.getUsersWhoBets()){
            if(participant.isRealUser()){
                tokens.add(participant.getAssociatedUser().getTokenId());
            }
        }

        for(Participant participant : bet.getUsersWhoReceive()){
            if(participant.isRealUser()){
                tokens.add(participant.getAssociatedUser().getTokenId());
            }
        }

        tokens.add(bet.getCreatedBy().getTokenId());

        for(String token : tokens) {
            notifyUsers(token, "Apuesta Resuelta", "Se ha resuelto la apuesta \"" + bet.getTitle()+ "\". A pagar!");
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

    private void notifyUsers(String token, String title, String message){

        String topic = "/topics/bet_notifications";
        FirebaseMessaging.getInstance().subscribeToTopic(topic);

        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put("title", title);
            notificationBody.put("message", message);
            notification.put("to", token);
            notification.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("TAG", "onCreate: " + e.getMessage());
        }

        sendNotification(notification);

    }

    private void sendNotification(JSONObject notification){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification, response -> {
            Log.i("TAG", "onResponse: " + response);
        }, error -> {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
            Log.i("TAG", "onErrorResponse: Didn't work", error);
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization",  "key=" + Constants.KEY);
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
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
        stateView.setText(bet.isPending() ?
                this.getResources().getString(R.string.pending) : bet.isWon() ?
                    this.getResources().getString(R.string.won_by_favour) : this.getResources().getString(R.string.won_by_against));
        createdByView.setText(bet.getCreatedBy().getFullName());
        if(bet.getDueDate() != null) {
            limitDateView.setText(new SimpleDateFormat("dd/MM/yyyy").format(bet.getDueDate()));
        }
        if(bet.getCreationDate() != null) {
            createdAtView.setText(new SimpleDateFormat("dd/MM/yyyy").format(bet.getCreationDate()));
        }
        favourAdapter.setParticipantList(bet.getUsersWhoBets());
        againstAdapter.setParticipantList(bet.getUsersWhoReceive());
    }

}
