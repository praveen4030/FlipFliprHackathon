package com.kraigs.fliprhackathon.Boards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.kraigs.fliprhackathon.Model.InvitedCards;
import com.kraigs.fliprhackathon.R;

public class InvitedCardsActivity extends AppCompatActivity {

    RecyclerView inviteRv;
    CollectionReference inviteRef;
    String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_cards);
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        inviteRef = FirebaseFirestore.getInstance().collection("User").document(currentUserId).collection("Invited cards");

        inviteRv = findViewById(R.id.invitedcardsRv);
        inviteRv.setLayoutManager(new LinearLayoutManager(this));
        setUpRv();
        
        
    }

    private void setUpRv() {

        Query query = inviteRef.orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<InvitedCards> options = new FirestoreRecyclerOptions.Builder<InvitedCards>()
                .setQuery(query,InvitedCards.class)
                .build();

        FirestoreRecyclerAdapter<InvitedCards,InvitedHolder> adapter = new FirestoreRecyclerAdapter<InvitedCards, InvitedHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull InvitedHolder holder, int position, @NonNull InvitedCards model) {

                holder.userTv.setText("Joined By: ");
                holder.boardTv.setText(model.getBoard());
                holder.cardTv.setText(model.getCard());
                holder.listTv.setText(model.getList());

            }

            @NonNull
            @Override
            public InvitedHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_invited_cards,parent,false);
                return  new InvitedHolder(v);
            }
        };

        inviteRv.setAdapter(adapter);
        adapter.startListening();

    }

    private class InvitedHolder extends RecyclerView.ViewHolder {
        TextView listTv,boardTv,cardTv,userTv;
        public InvitedHolder(@NonNull View itemView) {
            super(itemView);

            listTv = itemView.findViewById(R.id.listTv);
            cardTv = itemView.findViewById(R.id.cardTv);
            boardTv = itemView.findViewById(R.id.boardTv);
            userTv = itemView.findViewById(R.id.userTv);
        }
    }
}
