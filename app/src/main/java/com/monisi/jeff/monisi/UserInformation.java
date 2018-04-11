package com.monisi.jeff.monisi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserInformation extends AppCompatActivity {
    public static final String USER_NAME = "com.monisi.jeff.monisi.artistname";
    public static final String USER_ID = "com.monisi.jeff.monisi.artistid";




    EditText editTextName;
    Spinner spinnerLastName;
    Button buttonAddUser;
    ListView listViewUsers;
TextView textView1;
    //a list to store all the artist from firebase database
    List<User> users;

    //our database reference object
    DatabaseReference databaseUsers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_information);


        databaseUsers = FirebaseDatabase.getInstance().getReference("users");

        //getting views 
        editTextName = (EditText) findViewById(R.id.editTextName);
        spinnerLastName = (Spinner) findViewById(R.id.spinnerLastName);
        listViewUsers = (ListView) findViewById(R.id.listeViewUsers);
textView1 = findViewById(R.id.textView1);
        buttonAddUser = (Button) findViewById(R.id.buttonAddUser);

        //list to store artists
        users = new ArrayList<>();


        //adding an onclicklistener to button 
        buttonAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //calling the method addArtist()
                //the method is defined below 
                //this method is actually performing the write operation
                addUser();
            }


        });
    }
        private void addUser() {


            String name = editTextName.getText().toString().trim();
           // String lastName = spinnerLastName.getSelectedItem().toString();

            //checking if the value is provided
            if (!TextUtils.isEmpty(name)) {

                //getting a unique id using push().getKey() method
                //it will create a unique id and we will use it as the Primary Key for our Artist
                String id = databaseUsers.push().getKey();

                //creating an Artist Object
                User user = new User(id, name);

                //Saving the Artist
                databaseUsers.child(id).setValue(user);

                //setting edittext to blank again
                editTextName.setText("");

                //displaying a success toast
                Toast.makeText(this, "User added", Toast.LENGTH_LONG).show();
            } else {
                //if the value is not given displaying a toast
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();
            }
            databaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        //getting artist
                        User user = postSnapshot.getValue(User.class);
                        //adding artist to the list
                      textView1.setText(user.getUserId()+"    "+user.getUserName());


                    }


                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("message", "Failed to read value.", error.toException());
                }
            });
        }
    }

