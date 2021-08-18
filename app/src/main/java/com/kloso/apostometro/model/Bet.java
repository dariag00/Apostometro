package com.kloso.apostometro.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bet implements Serializable {

    private String id;
    private List<Participant> usersInFavour;
    private List<Participant> usersAgainst;
    private String title;
    private String reward;
    private List<String > participantEmails;
    private Date creationDate;
    private Date dueDate;
    private User createdBy;
    private State state;
    private Result result;

    public Bet(){
        participantEmails = new ArrayList<>();
        state = State.OPEN;
        result = Result.UNRESOLVED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Participant> getUsersInFavour() {
        return usersInFavour;
    }

    public void setUsersInFavour(List<Participant> usersInFavour) {

        for(Participant participant: usersInFavour){
            if(participant.isRealUser()){
                participantEmails.add(participant.getAssociatedUser().getEmail());
            }
        }

        this.usersInFavour = usersInFavour;
    }

    public List<Participant> getUsersAgainst() {
        return usersAgainst;
    }

    public void setUsersAgainst(List<Participant> usersAgainst) {
        for(Participant participant: usersAgainst){
            if(participant.isRealUser()){
                participantEmails.add(participant.getAssociatedUser().getEmail());
            }
        }
        this.usersAgainst = usersAgainst;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }


    public boolean haveUserWon(String user){

        boolean userInFavour = false;
        //TODO refactorizar basandose en el contains
        for(Participant participant : this.usersInFavour){
            if(participant.getName().equals(user)){
                userInFavour = true;
            }
        }

        return this.result == Result.WON_BY_FAVOUR && userInFavour
                || this.result == Result.WON_BY_AGAINST && !userInFavour;
    }


    public List<String> getParticipantEmails() {
        return participantEmails;
    }

    public void setParticipantEmails(List<String> participantEmails) {
        this.participantEmails = participantEmails;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getUsersInFavourString(){
        StringBuilder builder = new StringBuilder();
        for(Participant user : usersInFavour){
            builder.append(user.getName());
            builder.append(", ");
        }

        return  builder.toString().substring(0, builder.toString().length() -2);
    }

    public String getUsersAgainstString(){
        StringBuilder builder = new StringBuilder();
        for(Participant user : usersAgainst){
            builder.append(user.getName());
            builder.append(", ");
        }

        return  builder.toString().substring(0, builder.toString().length() -2);
    }

    @Exclude
    public Set<String> getParticipantTokens(){
        Set<String> tokens = new HashSet<>();

        for(Participant participant : this.usersInFavour){
            if(participant.isRealUser()){
                tokens.add(participant.getAssociatedUser().getTokenId());
            }
        }

        for(Participant participant : this.usersAgainst){
            if(participant.isRealUser()){
                tokens.add(participant.getAssociatedUser().getTokenId());
            }
        }

        tokens.add(this.createdBy.getTokenId());


        return tokens;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
