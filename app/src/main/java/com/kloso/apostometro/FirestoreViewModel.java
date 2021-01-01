package com.kloso.apostometro;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.User;

import java.util.ArrayList;
import java.util.List;

public class FirestoreViewModel extends ViewModel {

    private final String TAG = FirestoreViewModel.class.getName();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private BetRepository betRepository = new BetRepository();
    private MutableLiveData<List<Bet>> savedBets = new MutableLiveData<>();
    private MutableLiveData<List<User>> savedUsers = new MutableLiveData<>();
    private MutableLiveData<Bet> betLiveData = new MutableLiveData<>();

    public void saveUser(User user){
        betRepository.saveUserData(user).addOnFailureListener(runnable -> {
            Log.e(TAG, "Failed to save User");
        }).addOnSuccessListener(runnable -> {
            Log.i(TAG, "User saved correctly");
        });
    }

    public MutableLiveData<User> getUserByEmail(String email){
        betRepository.getUserByEmail(email).addOnCompleteListener(runnable -> {
            if(runnable.isSuccessful()){
                currentUser.setValue(runnable.getResult().toObject(User.class));
            }
        });

        return currentUser;
    }

    public LiveData<Bet> getBetLiveData(String betId){
        betRepository.getBet(betId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Get Expense Group Snapshot Listener Failed:", error);
            } else {
                Log.i(TAG, "Expense Group obtained correctly: " + value.toString());
                betLiveData.setValue(value.toObject(Bet.class));
            }
        });

        return betLiveData;
    }

    public LiveData<List<Bet>> getBets(){
        betRepository.getBets().addSnapshotListener((value, error) -> {
            if (error != null){
                Log.w(TAG, "Snapshot listener failed: ", error);
                savedBets.setValue(null);
            } else {
                Log.i(TAG, "Expense groups obtained correctly");
                List<Bet> expenseGroups = new ArrayList<>();
                for(QueryDocumentSnapshot document : value){
                    expenseGroups.add(document.toObject(Bet.class));
                }

                savedBets.setValue(expenseGroups);
            }
        });

        return savedBets;
    }


    public LiveData<List<User>> getUsers(){
        betRepository.getUsers().addSnapshotListener((value, error) -> {
            if (error != null){
                Log.w(TAG, "Snapshot listener failed: ", error);
                savedUsers.setValue(null);
            } else {
                Log.i(TAG, "Users obtained correctly");
                List<User> users = new ArrayList<>();
                for(QueryDocumentSnapshot document : value){
                    users.add(document.toObject(User.class));
                }

                savedUsers.setValue(users);
            }
        });

        return savedUsers;
    }

    public void saveBet(Bet bet){
        betRepository.saveBet(bet).addOnFailureListener(runnable -> {
            Log.e(TAG, "Failed to save Bet");
        }).addOnSuccessListener(runnable -> {
            Log.i(TAG, "Bet saved correctly");
        });
    }

    public void updateBet(Bet bet){
        betRepository.updateBet(bet);
    }

}
