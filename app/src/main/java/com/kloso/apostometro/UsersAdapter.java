package com.kloso.apostometro;

import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.kloso.apostometro.model.Participant;
import com.kloso.apostometro.model.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ParticipantGroupViewHolder>{

    private List<Participant> participantList;
    private List<User> users;

    private ParticipantLongClickListener participantLongClickListener;

    public  UsersAdapter(){
        participantList = new ArrayList<>();
        users = new ArrayList<>();
    }

    public UsersAdapter(ParticipantLongClickListener participantLongClickListener){
        this.participantLongClickListener = participantLongClickListener;
    }


    @NonNull
    @Override
    public ParticipantGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.participant_list_item, parent, false);
        return new ParticipantGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantGroupViewHolder holder, int position) {
        holder.itemView.setVisibility(View.VISIBLE);
        holder.bind(participantList.get(position));
    }

    @Override
    public int getItemCount() {
        return participantList != null ? participantList.size() : 0;
    }

    public void setParticipantList(List<Participant> participantList){
        this.participantList = participantList;
        notifyDataSetChanged();
    }

    public void setUsers(List<User> users){
        this.users = users;
        notifyDataSetChanged();
    }

    class ParticipantGroupViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{

        @BindView(R.id.tv_participant_name)
        TextView participantNameView;
        @BindView(R.id.profile_image)
        CircleImageView circleImageView;

        public ParticipantGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnLongClickListener(this);
            itemView.setVisibility(View.VISIBLE);
        }

        void bind(Participant participant){
            if(participant.isRealUser()){
                if(participant.getAssociatedUser().getProfilePicUri() != null){
                    Picasso.get().load(Uri.parse(participant.getAssociatedUser().getProfilePicUri())).error(R.drawable.ic_account_circle_black_24dp).into(circleImageView);
                }
            } else {
                circleImageView.setImageResource(R.drawable.ic_account_circle_black_24dp);
            }

            participantNameView.setText(participant.getName());

        }

        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            participantList.remove(participantList.get(position));
            notifyItemRemoved(position);
            view.setVisibility(View.GONE);
            notifyItemRangeChanged(position,participantList.size());
            return true;
        }

    }

    public interface ParticipantLongClickListener {
        void onParticipantClick(Participant clickedParticipant, int clickedPosition);
    }


}
