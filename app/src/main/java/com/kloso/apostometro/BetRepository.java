package com.kloso.apostometro;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.Result;
import com.kloso.apostometro.model.State;
import com.kloso.apostometro.model.User;

public class BetRepository {


    private final String TAG = this.getClass().getName();
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

    public Task<Void> saveUserData(User user){
        DocumentReference documentReference = firebaseFirestore.collection("users").document(user.getEmail());
        return documentReference.set(user);
    }

    public Task<DocumentSnapshot> getUserByEmail(String email){
        DocumentReference documentReference = firebaseFirestore.collection("users").document(email);
        return documentReference.get();
    }

    public Task<Void> deleteBet(Bet bet){
        return firebaseFirestore.collection("bets").document(bet.getId()).delete();
    }

    public Task<Void> saveBet(Bet bet){
        DocumentReference documentReference = firebaseFirestore.collection("bets").document();
        bet.setId(documentReference.getId());
        return documentReference.set(bet);
    }

    public Task<Void> updateBet(Bet bet){
        return firebaseFirestore.collection("bets").document(bet.getId()).set(bet);
    }

    public Query getBets(){
        return firebaseFirestore.collection("bets").whereArrayContains("participantEmails", firebaseUser.getEmail());
    }

    public Query getUsers(){
        return firebaseFirestore.collection("users");
    }


    public FirebaseUser getFirebaseUser(){
        return firebaseUser;
    }

    public DocumentReference getBet(String betId){
        return firebaseFirestore.collection("bets").document(betId);
    }

    public void updateItems(){
        firebaseFirestore.collection("bets").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        Bet bet = document.toObject(Bet.class);
                        if(bet.getState() == State.WON_BY_FAVOUR) {
                            bet.setState(State.RESOLVED);
                            bet.setResult(Result.WON_BY_FAVOUR);
                        } else if(bet.getState() == State.WON_BY_AGAINST) {
                            bet.setState(State.RESOLVED);
                            bet.setResult(Result.WON_BY_AGAINST);
                        } else if(bet.getState() == State.PAID) {
                            bet.setState(State.PAID);
                            bet.setResult(Result.WON_BY_FAVOUR);
                        }
                            String id = document.getId();
                        firebaseFirestore.collection("bets").document(id).set(bet); //Set student object
                    }
                }
            }
        });
    }

}
