package com.kloso.apostometro.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kloso.apostometro.BetRepository;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.DatePickerFragment;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.R;
import com.kloso.apostometro.UsersAdapter;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.Participant;
import com.kloso.apostometro.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CreateBetActivity extends AppCompatActivity implements UsersAdapter.ParticipantLongClickListener {

    private final String TAG = this.getClass().getName();

    @BindView(R.id.et_cb_title)
    EditText titleView;
    @BindView(R.id.et_cb_reward)
    EditText rewardView;
    @BindView(R.id.et_ce_date)
    EditText dateView;
    @BindView(R.id.et_end_date)
    EditText endDateView;
    @BindView(R.id.rv_users_in_favour)
    RecyclerView favourRecyclerView;
    @BindView(R.id.rv_users_against)
    RecyclerView againstRecyclerView;
    @BindView(R.id.toggle_button)
    ToggleButton toggleButton;
    @BindView(R.id.button_add_participant)
    Button addParticipantButton;
    @BindView(R.id.et_participant_name)
    EditText participantDataView;
    @BindView(R.id.pb_create_bet)
    ProgressBar progressBar;
    @BindView(R.id.form_login)
    LinearLayout formLogin;

    private List<Participant> favourParticipants;
    private UsersAdapter favourAdapter;
    private List<Participant> againstParticipants;
    private UsersAdapter againstAdapter;
    private List<User> currentUsers;
    private FirestoreViewModel firestoreViewModel;
    private User user;
    private String FCM_API = "https://fcm.googleapis.com/fcm/send";

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bet);

        ButterKnife.bind(this);

        againstParticipants = new ArrayList<>();
        favourParticipants = new ArrayList<>();
        favourAdapter = new UsersAdapter();
        againstAdapter = new UsersAdapter();

        dateView.setOnClickListener(view -> showDatePickerDialog(dateView));
        dateView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        endDateView.setOnClickListener(view -> showDatePickerDialog(endDateView));

        changeFormVisibility(true);

        this.setTitle("Create Bet");

        addParticipantButton.setOnClickListener( view -> {
            String text = participantDataView.getText().toString();
            if(!text.isEmpty()) {
                Participant participant = new Participant();
                User user = checkIfUserExists(text);
                if (user != null) {
                    participant.setAssociatedUser(user);
                    participant.setName(user.getFullName());
                    participant.setRealUser(true);
                } else {
                    participant.setName(text);
                }
                if (toggleButton.isChecked()) {
                    favourParticipants.add(participant);
                    favourAdapter.setParticipantList(favourParticipants);
                } else {
                    againstParticipants.add(participant);
                    againstAdapter.setParticipantList(againstParticipants);
                }
                participantDataView.setText("");
            } else {
                Toast.makeText(this, "No se puede crear un participant con nombre vacío.", Toast.LENGTH_SHORT).show();
            }
        });

        firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
        firestoreViewModel.getUsers().observe(this, users -> {
            Log.i(TAG, "Received new Users data. Size: " + users.size());
            currentUsers = users;
            favourAdapter.setUsers(users);
            againstAdapter.setUsers(users);
        });

        firestoreViewModel.getUserByEmail(new BetRepository().getFirebaseUser().getEmail()).observe(this, user -> {
            this.user = user;
        });

        setUpRecyclerView(favourRecyclerView, favourAdapter);
        setUpRecyclerView(againstRecyclerView, againstAdapter);

        requestQueue = Volley.newRequestQueue(this);

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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_bet_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int clickedItem = item.getItemId();
        if(clickedItem == R.id.action_send){
            processCreateExpenseAttempt();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private User checkIfUserExists(String id){
        for(User user : currentUsers){
            if(user.getEmail().equals(id)){
                return user;
            }
        }
        return null;
    }

    private void showDatePickerDialog(EditText view){
        DatePickerFragment datePickerFragment = DatePickerFragment.newInstance((datePicker, year, month, day) -> {
            String selectedDate = day + "/" + (month + 1) + "/" + year;
            view.setText(selectedDate);
        });
        datePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    private void processCreateExpenseAttempt(){
        if(isValidBetCreation()){
            try {
                changeFormVisibility(false);
                String title = titleView.getText().toString();
                String reward = rewardView.getText().toString();
                String dateString = dateView.getText().toString();

                Bet bet = new Bet();
                bet.setTitle(title);
                bet.setReward(reward);
                bet.setCreationDate(new SimpleDateFormat("dd/MM/yyyy").parse(dateString));
                if(endDateView.getText() != null) {
                    String endDateString = endDateView.getText().toString();
                    if(!endDateString.isEmpty()) {
                        bet.setDueDate(new SimpleDateFormat("dd/MM/yyyy").parse(endDateString));
                    }
                }

                bet.setUsersWhoBets(favourParticipants);
                bet.setUsersWhoReceive(againstParticipants);
                bet.setCreatedBy(user);
                bet.getParticipantEmails().add(user.getEmail());

                FirestoreViewModel viewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
                viewModel.saveBet(bet);

                changeFormVisibility(true);

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
                    notifyUsers(token, "Nueva apuesta creada", bet.getCreatedBy().getFullName() + " ha creado una apuesta y te ha añadido.");
                }
                finish();
            } catch (ParseException e) {
                Log.e(TAG, "Error while saving the bet: ", e);
                Toast.makeText(this, "Error while saving the bet. Please, try again later.", Toast.LENGTH_SHORT).show();
                changeFormVisibility(true);
            }
        } else {
            Toast.makeText(this, "Rellena los campos necesarios!", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeFormVisibility(boolean showLoginForm){
        formLogin.setVisibility(showLoginForm ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(showLoginForm ? View.GONE :  View.VISIBLE);
    }

    private boolean isValidBetCreation(){
        boolean isValid = true;

        if(rewardView.getText().toString().isEmpty()){
            isValid = false;
        } else if(titleView.getText().toString().isEmpty()){
            isValid = false;
        } else if(favourParticipants.isEmpty() || againstParticipants.isEmpty()){
            isValid = false;
        }

        return isValid;
    }


    @Override
    public void onParticipantClick(Participant clickedParticipant, int clickedPosition) {
    }
}
