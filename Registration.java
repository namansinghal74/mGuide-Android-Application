package com.example.mguide;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.mguide.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class Registration extends AppCompatActivity {
    private EditText name, email, phone, pass;
    private Button register;
    private TextView signin;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore mDb;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        signin=(TextView) findViewById(R.id.signin);
        register=(Button) findViewById(R.id.reg);
        name=(EditText) findViewById(R.id.fullname);
        email=(EditText) findViewById(R.id.email);
        phone=(EditText) findViewById(R.id.phone);
        pass=(EditText) findViewById(R.id.password);
        progressDialog = new ProgressDialog(this);
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Registration.this, MainActivity.class);
                startActivity(intent);
            }
        });
        mDb = FirebaseFirestore.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(validate())
               {
                   final String _email=email.getText().toString().trim();
                   String password=pass.getText().toString();
                   final String _name=name.getText().toString();
                   final String _phone=phone.getText().toString();
                   progressDialog.setMessage("Registering You!");
                   progressDialog.show();

                   firebaseAuth.createUserWithEmailAndPassword(_email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if(task.isSuccessful())
                           {
                               Users user = new Users();
                               user.setEmail(_email);
                               user.setPhone(_phone);
                               user.setName(_name);
                               user.setUsername(_email.substring(0, _email.indexOf("@")));
                               user.setUser_id(FirebaseAuth.getInstance().getUid());

                               FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                                       .setTimestampsInSnapshotsEnabled(true)
                                       .build();
                               mDb.setFirestoreSettings(settings);
                               DocumentReference newUserRef = mDb
                                       .collection(getString(R.string.collection_users))
                                       .document(FirebaseAuth.getInstance().getUid());
                               newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                   @Override
                                   public void onComplete(@NonNull Task<Void> task) {
                                       if(task.isSuccessful())
                                       {
                                           progressDialog.dismiss();
                                           Toast.makeText(getApplicationContext(), "Registration successful", Toast.LENGTH_SHORT).show();
                                           startActivity(new Intent(Registration.this, MainActivity.class));
                                       }
                                        else
                                       {
                                           Toast.makeText(getApplicationContext(), "Already Registered. Please Sign In", Toast.LENGTH_SHORT).show();
                                           progressDialog.dismiss();
                                       }
                                   }
                               });

                           }
                           else
                           {
                               Toast.makeText(getApplicationContext(), "Please Enter Correct e-mail", Toast.LENGTH_SHORT).show();
                               progressDialog.dismiss();
                           }

                       }
                   });
               }
            }
        });
    }
    private Boolean validate()
    {
        boolean result=false;
        String fullname=name.getText().toString();
        String password=pass.getText().toString();
        String _email=email.getText().toString();
        String _phone=phone.getText().toString();
        if (_phone.length() != 10) {
            Toast.makeText(getApplicationContext(), "Please Enter 10 digit Mobile Number", Toast.LENGTH_SHORT).show();
        }
        else if(fullname.isEmpty() || password.isEmpty() || _email.isEmpty() || _phone.isEmpty())
        {
            Toast.makeText(getApplicationContext(), "Please Enter All The Details", Toast.LENGTH_SHORT).show();
        }
        else
        {
            result=true;
        }
        return result;
    }
}
