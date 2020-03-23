package com.kraigs.fliprhackathon.Boards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.kraigs.fliprhackathon.Model.Cards;
import com.kraigs.fliprhackathon.Model.List;
import com.kraigs.fliprhackathon.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BoardDetailActivity extends AppCompatActivity {

    RecyclerView listRv;
    String currentUserId;
    CollectionReference listRef;
    String board;
    FloatingActionButton listFab;
    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        board = getIntent().getStringExtra("board");
        listFab = findViewById(R.id.fabList);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listRef = FirebaseFirestore.getInstance().collection("User").document(currentUserId).collection("Boards").document(board).collection("Lists");
        loadingBar = new ProgressDialog(this);

        listRv = findViewById(R.id.listRv);
        listRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        setUpRv();

        listFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                listDialog();

            }
        });

    }

    private void setUpRv() {

        Query query = listRef;
        FirestoreRecyclerOptions<List> options = new FirestoreRecyclerOptions.Builder<List>()
                .setQuery(query, List.class)
                .build();

        FirestoreRecyclerAdapter<List, ListHolder> adapter = new FirestoreRecyclerAdapter<List, ListHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ListHolder holder, int position, @NonNull List model) {

                String list = getSnapshots().getSnapshot(position).getId();

                holder.cardRv.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));

                CardAdapter adapter1 = new CardAdapter(model.getCardList(),list);
                holder.cardRv.setAdapter(adapter1);
                adapter1.notifyDataSetChanged();

                holder.addcardTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addCardDialog(list, model.getCardList());
                    }
                });

                holder.listNameTv.setText(list);

            }

            @NonNull
            @Override
            public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_list, parent, false);
                return new ListHolder(v);
            }
        };

        listRv.setAdapter(adapter);
        adapter.startListening();

    }

    class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {

        ArrayList<String> list;
        String listName;

        public CardAdapter(ArrayList<String> list,String listName) {
            this.list = list;
            this.listName = listName;
        }

        @NonNull
        @Override
        public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.zunit_card, parent, false);
            return new CardHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CardHolder holder, int position) {

            String card = list.get(position);
            holder.cardTv.setText(card);
            holder.cardTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(BoardDetailActivity.this, CardsActivity.class);
                    intent.putExtra("card", card);
                    intent.putExtra("board",board);
                    intent.putExtra("list",listName);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private class CardHolder extends RecyclerView.ViewHolder {
            TextView cardTv;

            public CardHolder(@NonNull View itemView) {
                super(itemView);

                cardTv = itemView.findViewById(R.id.cardTv);

            }
        }
    }

    private void addCardDialog(String list, ArrayList<String> cardList) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_list);

        Button saveBt = dialog.findViewById(R.id.saveBt);
        TextInputEditText cardEt = dialog.findViewById(R.id.listEt);
        TextView rawTv = dialog.findViewById(R.id.addRawTv);
        rawTv.setText("Add card");
        cardEt.setHint("Card name");

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String card = cardEt.getText().toString();
                if (!TextUtils.isEmpty(card)) {

                    cardList.add(card);

                    HashMap<String, Object> map1 = new HashMap<>();
                    map1.put("cardList", cardList);

                    listRef.document(list).update(map1).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                HashMap<String,Object> map = new HashMap<>();
                                map.put("timestamp",FieldValue.serverTimestamp());

                                listRef.document(list).collection("Cards").document(card).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            Toast.makeText(BoardDetailActivity.this, "Card added successfully!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                dialog.dismiss();
                            }
                        }
                    });

                } else {
                    Toast.makeText(BoardDetailActivity.this, "Select all fields and continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void listDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.zlayout_list);

        Button saveBt = dialog.findViewById(R.id.saveBt);
        TextInputEditText listEt = dialog.findViewById(R.id.listEt);
        listEt.setHint("List Name");

        saveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String list = listEt.getText().toString();
                if (!TextUtils.isEmpty(list)) {
                    loadingBar.setTitle("List");
                    loadingBar.setMessage("Please wait!");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("timestamp", FieldValue.serverTimestamp());

                    listRef.document(list).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                loadingBar.dismiss();
                                Toast.makeText(BoardDetailActivity.this, "Board added successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                loadingBar.dismiss();
                                dialog.dismiss();
                                Toast.makeText(BoardDetailActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    loadingBar.dismiss();
                    Toast.makeText(BoardDetailActivity.this, "Select all fields and continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();

    }

    private class ListHolder extends RecyclerView.ViewHolder {

        TextView addcardTv, listNameTv;
        RecyclerView cardRv;

        public ListHolder(@NonNull View itemView) {
            super(itemView);

            cardRv = itemView.findViewById(R.id.cardRv);
            addcardTv = itemView.findViewById(R.id.addCardTv);
            listNameTv = itemView.findViewById(R.id.listNameTv);
        }
    }

}
