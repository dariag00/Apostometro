package com.kloso.apostometro.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kloso.apostometro.BetRepository;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.DatePickerFragment;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.NotificationUtils;
import com.kloso.apostometro.R;
import com.kloso.apostometro.UsersAdapter;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.Participant;
import com.kloso.apostometro.model.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    private boolean isEdit;
    private Bet editableBet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bet);

        ButterKnife.bind(this);

        againstParticipants = new ArrayList<>();
        favourParticipants = new ArrayList<>();
        favourAdapter = new UsersAdapter();
        againstAdapter = new UsersAdapter();

        Intent previousIntent = getIntent();
        if(previousIntent.hasExtra(Constants.BET)) {
            editableBet = (Bet) previousIntent.getSerializableExtra(Constants.BET);
            isEdit = true;
            this.setTitle(this.getResources().getString(R.string.edit_bet));
            populateFormWithBetData();
        } else {
            isEdit = false;
            dateView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            this.setTitle(this.getResources().getString(R.string.create_bet));
        }

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


        dateView.setOnClickListener(view -> showDatePickerDialog(dateView));
        endDateView.setOnClickListener(view -> showDatePickerDialog(endDateView));

        changeFormVisibility(true);

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
                Toast.makeText(this, this.getResources().getString(R.string.empty_participant_error), Toast.LENGTH_SHORT).show();
            }
        });

        setUpRecyclerView(favourRecyclerView, favourAdapter);
        setUpRecyclerView(againstRecyclerView, againstAdapter);

        requestQueue = Volley.newRequestQueue(this);

    }

    private void populateFormWithBetData(){
        titleView.setText(editableBet.getTitle());
        rewardView.setText(editableBet.getReward());
        if(editableBet.getDueDate() != null) {
            dateView.setText(new SimpleDateFormat("dd/MM/yyyy").format(editableBet.getDueDate()));
        }
        if(editableBet.getCreationDate() != null) {
            endDateView.setText(new SimpleDateFormat("dd/MM/yyyy").format(editableBet.getCreationDate()));
        }
        favourParticipants.addAll(editableBet.getUsersInFavour());
        againstParticipants.addAll(editableBet.getUsersAgainst());
        favourAdapter.setParticipantList(favourParticipants);
        againstAdapter.setParticipantList(againstParticipants);
    }

    private void processCreateExpenseAttempt(){
        if(isValidBetCreation()){
            try {
                changeFormVisibility(false);
                String title = titleView.getText().toString();
                String reward = rewardView.getText().toString();
                String dateString = dateView.getText().toString();

                Bet bet = isEdit ? editableBet : new Bet();
                bet.setTitle(title);
                bet.setReward(reward);
                bet.setCreationDate(new SimpleDateFormat("dd/MM/yyyy").parse(dateString));
                if(endDateView.getText() != null) {
                    String endDateString = endDateView.getText().toString();
                    if(!endDateString.isEmpty()) {
                        bet.setDueDate(new SimpleDateFormat("dd/MM/yyyy").parse(endDateString));
                    }
                }

                bet.setUsersInFavour(favourParticipants);
                bet.setUsersAgainst(againstParticipants);
                if(!isEdit) {
                    bet.setCreatedBy(user);
                }

                FirestoreViewModel viewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
                if(isEdit){
                    bet.getParticipantEmails().add(user.getEmail());
                    viewModel.updateBet(bet);
                } else {
                    viewModel.saveBet(bet);
                }

                changeFormVisibility(true);

                Set<String> tokens = bet.getParticipantTokens();
                tokens = tokens.stream().filter(token -> !token.equals(user.getTokenId())).collect(Collectors.toSet());

                NotificationUtils notificationUtils = new NotificationUtils(this, Volley.newRequestQueue(this));

                for(String token : tokens) {
                    if(!isEdit) {
                        notificationUtils.notifyUsers(token, "Nueva apuesta creada", bet.getCreatedBy().getFullName() + " ha creado una apuesta y te ha añadido.", null);
                    } else {
                        notificationUtils.notifyUsers(token, "Apuesta Editada", user.getFullName() + " ha editado una apuesta en la que estás añadido", bet.getId());
                    }
                }
                finish();
            } catch (ParseException e) {
                Log.e(TAG, "Error while saving the bet: ", e);
                Toast.makeText(this, this.getResources().getString(R.string.saving_bet_error), Toast.LENGTH_SHORT).show();
                changeFormVisibility(true);
            }
        } else {
            Toast.makeText(this, "Rellena los campos necesarios", Toast.LENGTH_SHORT).show();
        }
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
