package com.kloso.apostometro;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.kloso.apostometro.model.Bet;
import com.kloso.apostometro.model.State;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BetsAdapter extends RecyclerView.Adapter<BetsAdapter.BetViewHolder> implements SwipeToDeleteCallback.SwipeListener{

    private List<Bet> betList;
    private Activity activity;
    private BetClickListener betClickListener;
    private String currentParticipant;

    public BetsAdapter(Activity activity, BetClickListener clickListener, String currentParticipant){
        this.activity = activity;
        this.currentParticipant = currentParticipant;
        this.betClickListener = clickListener;
    }

    @NonNull
    @Override
    public BetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bet_list_item, parent, false);
        return new BetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BetViewHolder holder, int position) {
        holder.bind(betList.get(position), activity.getApplicationContext());
    }

    @Override
    public int getItemCount() {
        return betList != null ? betList.size() : 0;
    }

    public void setBetList(List<Bet> betList){
        this.betList = betList;
        notifyDataSetChanged();
    }

    @Override
    public void deleteItem(int position) {
        Log.i("TAG", "Bet delete");
        showConfirmationDialog(position);
    }

    private void showConfirmationDialog(int position) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this.activity);
        alert.setTitle(this.activity.getResources().getString(R.string.delete_bet));
        alert.setMessage(this.activity.getResources().getString(R.string.delete_bet_confirm_2));
        alert.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            delete(position);
        });
        alert.setNegativeButton(android.R.string.no, (dialog, which) -> {
            restoreItem(position);
            dialog.cancel();
        });
        alert.setOnCancelListener(dialogInterface -> {
            restoreItem(position);
        });
        alert.show();
    }

    private void delete(int position){
        Bet deletedBet = betList.get(position);
        betList.remove(position);
        new BetRepository().deleteBet(deletedBet);
        notifyItemRemoved(position);
    }

    private void restoreItem(int position){
        notifyItemChanged(position);
    }

    class BetViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.tv_participants_against)
        TextView participantsAgainstView;
        @BindView(R.id.tv_participants_favour)
        TextView participantsFavourView;
        @BindView(R.id.tv_thing_bet)
        TextView thingBetView;
        @BindView(R.id.tv_title)
        TextView betTitleView;
        @BindView(R.id.bet_result_indicator)
        View resultIndicator;

        public BetViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void bind(Bet bet, Context context) {
            thingBetView.setText(bet.getReward());
            betTitleView.setText(bet.getTitle());
            participantsAgainstView.setText(itemView.getContext().getResources().getString(R.string.against) +  " " +bet.getUsersAgainstString());
            participantsFavourView.setText(itemView.getContext().getResources().getString(R.string.favour) + " " + bet.getUsersInFavourString());
            if(bet.getState().equals(State.OPEN)){
                resultIndicator.setVisibility(View.INVISIBLE);
            } else if(bet.haveUserWon(currentParticipant)){
                resultIndicator.setBackgroundColor(context.getResources().getColor(R.color.victoryColor));
            } else {
                resultIndicator.setBackgroundColor(context.getResources().getColor(R.color.defeatColor));
            }
        }

        @Override
        public void onClick(View view) {
            betClickListener.onBetClick(betList.get(getAdapterPosition()));
        }
    }

    public interface BetClickListener {
        void onBetClick(Bet clickedBet);
    }

}
