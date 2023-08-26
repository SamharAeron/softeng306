package com.softeng306team15.plantoid.Activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.softeng306team15.plantoid.Models.IUser;
import com.softeng306team15.plantoid.Models.User;
import com.softeng306team15.plantoid.MyCallback;
import com.softeng306team15.plantoid.R;

public class CreateAccountActivity  extends AppCompatActivity {

    private class ViewHolder {

        TextView createErrorText;

        EditText passText, confirmPassText, emailText;

        Button btnCreate, btnLogin;

        public ViewHolder() {
            createErrorText = findViewById(R.id.textErrorCreate);

            passText = findViewById(R.id.editPassword);
            confirmPassText = findViewById(R.id.editPasswordConfirm);
            emailText = findViewById(R.id.editEmail);

            btnCreate = findViewById(R.id.btnCreateAccount);
            btnLogin = findViewById(R.id.btnLogIn);
        }
    }

    ViewHolder vh;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        vh = new ViewHolder();

        vh.btnCreate.setOnClickListener(this::verifyInputs);

        vh.btnLogin.setOnClickListener(this::goLogin);
    }

    public void createAccount(String email, String password) {
        mAuth = FirebaseAuth.getInstance();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        IUser userInfo = new User(email);
        userInfo.createNewUserDocument(new MyCallback(){
            @Override
            public void onCallback() {
                Intent mainIntent = new Intent(getBaseContext(), MainActivity.class);
                mainIntent.putExtra("User", userInfo.getId());
                startActivity(mainIntent);
            }
        });
    }

    public void verifyInputs(View v) {
        // Check if passwords match
        String password = vh.passText.getText().toString();
        String confPassword = vh.confirmPassText.getText().toString();

        if (!password.equals(confPassword)) {
            vh.createErrorText.setText("Passwords must match");
            vh.passText.setText("");
            vh.confirmPassText.setText("");
            return;
        }

        String email = vh.emailText.getText().toString();

        if (password.equals("") || email.equals("")) {
            vh.createErrorText.setText("All fields must be filled");
            return;
        }

        createAccount(email, password);

    }

    public void goLogin(View v) {
        Intent loginIntent = new Intent(getBaseContext(), LogInActivity.class);
        startActivity(loginIntent);
    }
}