package com.kloso.apostometro.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Bet implements Serializable {

    private String id;
    private List<Participant> usersWhoBets;
    private List<Participant> usersWhoReceive;
    private String title;
    private String reward;
    private boolean won;
    private boolean pending;
    private List<String> participantEmails;
    private Date creationDate;
    private Date dueDate;
    private User createdBy;

    public Bet(){
        this.pending = true;
        participantEmails = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Participant> getUsersWhoBets() {
        return usersWhoBets;
    }

    public void setUsersWhoBets(List<Participant> usersWhoBets) {

        for(Participant participant: usersWhoBets){
            if(participant.isRealUser()){
                participantEmails.add(participant.getAssociatedUser().getEmail());
            }
        }

        this.usersWhoBets = usersWhoBets;
    }

    public List<Participant> getUsersWhoReceive() {
        return usersWhoReceive;
    }

    public void setUsersWhoReceive(List<Participant> usersWhoReceive) {
        for(Participant participant: usersWhoReceive){
            if(participant.isRealUser()){
                participantEmails.add(participant.getAssociatedUser().getEmail());
            }
        }
        this.usersWhoReceive = usersWhoReceive;
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

    public boolean isWon() {
        return won;
    }

    public boolean haveUserWon(String user){

        boolean userInFavour = false;

        for(Participant participant : this.usersWhoBets){
            if(participant.getName().equals(user)){
                userInFavour = true;
            }
        }

        return this.isWon() && userInFavour || !this.isWon() && !userInFavour;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public List<String> getParticipantEmails() {
        return participantEmails;
    }

    public void setParticipantEmails(List<String> participantEmails) {
        this.participantEmails = participantEmails;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
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

    public String getUsersInFavour(){
        StringBuilder builder = new StringBuilder();
        for(Participant user : usersWhoBets){
            builder.append(user.getName());
            builder.append(", ");
        }

        return  builder.toString().substring(0, builder.toString().length() -2);
    }

    public String getUsersAgainst(){
        StringBuilder builder = new StringBuilder();
        for(Participant user : usersWhoReceive){
            builder.append(user.getName());
            builder.append(", ");
        }

        return  builder.toString().substring(0, builder.toString().length() -2);
    }
}
