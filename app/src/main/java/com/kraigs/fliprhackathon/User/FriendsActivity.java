package com.kraigs.fliprhackathon.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.kraigs.fliprhackathon.Boards.CardsActivity;
import com.kraigs.fliprhackathon.Chat.ChatActivity;
import com.kraigs.fliprhackathon.Chat.User;
import com.kraigs.fliprhackathon.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsActivity extends AppCompatActivity {

    CollectionReference userCol;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        RecyclerView inviteRv = findViewById(R.id.friendsRv);
        inviteRv.setLayoutManager(new LinearLayoutManager(this));

        userCol = FirebaseFirestore.getInstance().collection("User");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = userCol.document(currentUserId).collection("Friends");

        FirestoreRecyclerOptions<User> options= new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query,User.class)
                .build();

        FirestoreRecyclerAdapter<User,FriendsAdapter> adapter = new FirestoreRecyclerAdapter<User, FriendsAdapter>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendsAdapter holder, int position, @NonNull User model) {
                holder.nameTv.setText(model.getName());
                Picasso.get().load(model.getImage()).placeholder(R.drawable.user_profile_image).into(holder.userProfileImage);
                holder.inviteBt.setVisibility(View.GONE);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(FriendsActivity.this, ChatActivity.class);
                        intent.putExtra("visit_user_id",model.getKey());
                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public FriendsAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_friends,parent,false);
                return new FriendsAdapter(v);
            }
        };

        inviteRv.setAdapter(adapter);
        adapter.startListening();

    }

    private class FriendsAdapter extends RecyclerView.ViewHolder {
        TextView nameTv;
        CircleImageView userProfileImage;
        Button inviteBt;

        public FriendsAdapter(@NonNull View itemView) {
            super(itemView);

            inviteBt = itemView.findViewById(R.id.inviteBt);
            userProfileImage = itemView.findViewById(R.id.users_profile_image);
            nameTv = itemView.findViewById(R.id.nameTv);
        }
    }
}
