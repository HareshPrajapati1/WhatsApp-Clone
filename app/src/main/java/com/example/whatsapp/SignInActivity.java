package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.whatsapp.Models.Users;
import com.example.whatsapp.databinding.ActivitySignInBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.core.Tag;

public class SignInActivity extends AppCompatActivity {
    //this is id for sign in(Same as findViewById())
    ActivitySignInBinding binding;

    ProgressDialog progressDialog;
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;

    GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        progressDialog = new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Please Wait\nValidation in Progress.");

        //This code for a Google SignIn (Shows a default email in signIn option)
        //Configure Google SignIn
        GoogleSignInOptions gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                                .build();
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso);



        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!binding.txtEmail.getText().toString().isEmpty() &&
                        !binding.txtPassword.getText().toString().isEmpty()) {
                    progressDialog.show();
                    //this is by default method of Firebase
                    mAuth.signInWithEmailAndPassword(binding.txtEmail.getText().toString(), binding.txtPassword.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressDialog.dismiss();
                                    if (task.isSuccessful()) {
                                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                        startActivity(intent);

                                    } else {
                                        //this show exception from the firebase
                                        Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                } else {
                    Toast.makeText(SignInActivity.this, "Enter Credential", Toast.LENGTH_SHORT).show();
                }


            }
        });
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
        }
        binding.txtClickSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
        //Google
        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

    }

    int RC_SIGN_IN=65;

    private void signIn(){
        Intent signInIntent= mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        //Result returned from launching the Intent From GoogleSignInApi.getSignInIntent(...);

        if (requestCode==RC_SIGN_IN){
            Task<GoogleSignInAccount> task=GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                //Google signIn was Successful,authenticate with firebase
                GoogleSignInAccount account=task.getResult(ApiException.class);
                Log.d("TAG","firebaseAuthWithGoogle" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            }catch (ApiException e){
             Log.d("TAG","Google Sign in failed",e);
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken){
        AuthCredential credential= GoogleAuthProvider.getCredential(idToken,null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                      if(task.isSuccessful()){
                          //Sign in success,update UI with the signed-in user's information
                          Log.d("TAG","signInWithCredential:success");
                          FirebaseUser user=mAuth.getCurrentUser();

                          //Store data to real time data base
                          Users users=new Users();
                          users.setUserId(user.getUid());
                          users.setUserName(user.getDisplayName());
                          users.setProfilePic(user.getPhotoUrl().toString());//Store  Profile
                          firebaseDatabase.getReference().child("Users").child(user.getUid()).setValue(users);


                          //Sign in is success full than move to mainActivity
                          Intent intent=new Intent(SignInActivity.this,MainActivity.class);
                          startActivity(intent);
                          Toast.makeText(SignInActivity.this, "Sign In With Google", Toast.LENGTH_SHORT).show();
                      }else {
                          //If sign in fails,display a message to user..
                          Log.d("TAG","signInWithCredential:failure",task.getException());
                      }
                    }
                });
    }
}