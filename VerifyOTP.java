package com.radio.chat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.radio.chat.R;
import com.radio.chat.chat.models.User_FB;
import com.radio.chat.data.RadioPreferences;
import com.radio.chat.rest.APIExecutor;
import com.radio.chat.rest.model.otpmodel.OtpResponse;
import com.radio.chat.rest.model.otpmodel.VerifyResponse;
import com.radio.chat.rest.model.userdetailsmodel.UserDetails;
import com.radio.chat.utils.AppConstants;
import com.radio.chat.utils.AppUtils;
import com.radio.chat.utils.listeners.CommonInterface;
import com.radio.chat.utils.listeners.SMSListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Response;

public class VerifyOTP extends AppCompatActivity {

    @BindView(R.id.txtVerify)
    TextView txtVerify;

    @BindView(R.id.txtTo)
    TextView txtTo;

    @BindView(R.id.txtResendTime)
    TextView txtResendTime;

    @BindView(R.id.otpView)
    PinView otpView;
    RadioPreferences rp;
    private DatabaseReference mDatabase;
    boolean isNewuser = false;
    private String phone, timeStamp;
    private int otp, sms_otp;
    private Bundle bundle;
    private CountDownTimer timer;
    private AppUtils utils;
    Realm realm;
    private FirebaseAuth mAuth;
    UserDetails details;
    boolean isVerifyClicked = false, isAuthenticated = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);
        try {
            isNewuser = true;
            initialize();
            otpInitListener();
            setResendTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            // Check if user is signed in (non-null) and update UI accordingly.
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                //already authenticated
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        try {
            ButterKnife.bind(this);
            rp = RadioPreferences.getInstance(VerifyOTP.this);
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            utils = new AppUtils(VerifyOTP.this);
            realm = Realm.getDefaultInstance();
            bundle = getIntent().getExtras();
            if (bundle != null) {
                phone = bundle.getString(AppConstants.PHONE_NUMBER);
                //otp = bundle.getInt(AppConstants.OTP);
                txtTo.setText(getResources().getString(R.string.to) + " : " + phone);
                generateOTP();
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    private void otpInitListener() {
        try {
            SMSListener.bindListener(new CommonInterface.OTPListener() {
                @Override
                public void onOTPReceived(String extractedOTP) {
                    otpView.setText(extractedOTP);
                    otp = Integer.parseInt(extractedOTP);
                    if (isAuthenticated) {
                        verifyOTP(otp);
                    } else {
                        isVerifyClicked = true;
                        utils.showShortToast("Authenticating", AppUtils.ErrorToast);
                    }


                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setResendTimer() {
        try {
            if (timer != null) {
                timer.start();
                txtResendTime.setEnabled(false);
            } else {

                timer = new CountDownTimer(15000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        txtResendTime.setText("OTP can be resend in " + millisUntilFinished / 1000 + " seconds");
                    }

                    public void onFinish() {
                        txtResendTime.setText(getResources().getString(R.string.resendotp));
                        txtResendTime.setEnabled(true);
                    }

                };
                timer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.txtResendTime)
    public void setTxtResendTime() {
        setResendTimer();
        generateOTP();
    }

    @OnClick(R.id.txtVerify)
    public void setTxtVerify() {
        try {
            if (utils.isNotEmptyString(otpView.getText().toString())) {
                otp = Integer.parseInt(otpView.getText().toString());
                Calendar c = Calendar.getInstance();
                System.out.println("Current time => " + c.getTime());

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                timeStamp = df.format(c.getTime());

                if (utils.isNotEmptyString("" + otp)) {
                    if (isAuthenticated) {
                        verifyOTP(otp);
                    } else {
                        isVerifyClicked = true;
                        utils.showShortToast("Authenticating", AppUtils.ErrorToast);
                    }
                } else {
                    utils.showShortToast("Missing OTP", AppUtils.ErrorToast);
                }
            } else {
                utils.showShortToast("Enter OTP", AppUtils.InfoToast);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void generateOTP() {
        try {
            utils.writeErrorLog("input " + phone);
            Call<OtpResponse> call = APIExecutor.getApiService().generateOTP(phone, true);
            utils.writeErrorLog("url " + call.request().url());

            call.enqueue(new retrofit2.Callback<OtpResponse>() {
                @Override
                public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                    try {
                        utils.writeErrorLog("generate response" + new Gson().toJson(response.body()));
                        if (response.body() != null) {
                            if (response.body().getStatus().equalsIgnoreCase("success")) {
                                //utils.showShortToast("OTP : " + response.body().getOtp().getCode(), AppUtils.InfoToast);
                                //utils.showShortToast("Success", AppUtils.SuccessToast);

                                utils.showShortToast("OTP : " + response.body().getOtp().getCode(), AppUtils.InfoToast);
                                utils.showShortToast("Success", AppUtils.SuccessToast);

                                //change isnewuser to isverified
                                rp.storeMember(response.body().getUserData(), response.body().isNewUser());
                                details = response.body().getUserData();
                                firebaseAuthenticate(response.body());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<OtpResponse> call, Throwable t) {
                    try {
                        utils.showShortToast("Failure", AppUtils.ErrorToast);
                        t.printStackTrace();
                        utils.writeErrorLog(t.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyOTP(int OTP_CODE) {
        try {
            Call<VerifyResponse> call = APIExecutor.getApiService().verifyOTP(phone, phone, OTP_CODE, timeStamp);
            utils.writeErrorLog("url " + call.request().url());
            call.enqueue(new retrofit2.Callback<VerifyResponse>() {
                @Override
                public void onResponse(Call<VerifyResponse> call, Response<VerifyResponse> response) {
                    try {
                        utils.writeErrorLog("verify response" + new Gson().toJson(response.body()));
                        if (response.body() != null) {
                            if (response.body().getStatus().equalsIgnoreCase("success")) {
                                VerifyResponse.OtpData otpData = response.body().getData();
                                utils.showShortToast("OTP verified", AppUtils.SuccessToast);
                                rp.setUserToken(otpData.getToken());
                                // TODO store user key from  otpdata in realm local db
                                if (isNewuser) {
                                    Intent i = new Intent(VerifyOTP.this, InitializeUser.class);
                                    i.putExtra("token", otpData.getToken());
                                    startActivity(i);
                                    finish();
                                } else {
                                    rp.setIsLoggedIn("true");
                                    Intent i = new Intent(VerifyOTP.this, DashboardActivity.class);
                                    i.putExtra("user", "existing");
                                    startActivity(i);
                                    finish();
                                }
                            } else {
                                utils.showShortToast("Verification Failed", AppUtils.SuccessToast);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<VerifyResponse> call, Throwable t) {
                    try {
                        utils.showShortToast("Failure", AppUtils.ErrorToast);
                        t.printStackTrace();
                        utils.writeErrorLog(t.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void firebaseAuthenticate(OtpResponse response) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.e("user_id", user.getUid());
                            if (user != null) {
                                isAuthenticated = true;
                                //rp.setIsLoggedIn("true");
                                if (!response.isNewUser()) {
                                    storeUserDetails(response.getUserData());
                                    //updateUser(response.body().getUserData());
                                    checkUserExistInFB(response.getUserData());
                                } else {
                                    checkUserExistInFB(response.getUserData());
                                    //addUserInFirebase(response.body().getUserData());
                                    storeUserDetails(response.getUserData());
                                }
                                isNewuser = response.isNewUser();
                                //otp = response.body().getOtp().getCode();
                                timeStamp = response.getUserData().getLastUpdated();
                            } else {
                                utils.showShortToast("Authentication failed.", AppUtils.ErrorToast);
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            utils.showShortToast("Authentication failed.", AppUtils.ErrorToast);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        SMSListener.unbindListener();
        super.onDestroy();
    }

    private void checkUserExistInFB(UserDetails userDetails) {
        try {
            mDatabase.getRef().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (data.child(userDetails.getId()).exists()) {
                            updateUser(userDetails);
                        } else {
                            addUserInFirebase(userDetails);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storeUserDetails(UserDetails data) {
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UserDetails userDetails = new UserDetails();
                    userDetails.setId(data.getId());
                    userDetails.setFirstName(data.getFirstName());
                    userDetails.setLastName(data.getLastName());
                    userDetails.setFullName(data.getFullName());
                    userDetails.setUsername(data.getUsername());
                    userDetails.setPhone(data.getPhone());
                    userDetails.setAbout(data.getAbout());
                    userDetails.setAvatar(data.getAvatar());
                    userDetails.setVerified(data.isVerified());
                    userDetails.setLastUpdated(data.getLastUpdated());
                    userDetails.setLocation(data.getLocation());
                    realm.copyToRealmOrUpdate(userDetails);
                    // realm.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUser(UserDetails data) {
        try {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(InstanceIdResult instanceIdResult) {
                    try {
                        Log.e("device token", instanceIdResult.getToken());
                        if (instanceIdResult.getToken() != null) {
                            HashMap<String, Object> usermap = new HashMap<>();
                            usermap.put("firstName", data.getFirstName());
                            usermap.put("lastName", data.getLastName());
                            usermap.put("fullName", data.getFullName());
                            usermap.put("username", data.getUsername());
                            usermap.put("phone", data.getPhone());
                            usermap.put("about", data.getAbout());
                            usermap.put("avatar", data.getAvatar());
                            usermap.put("lastUpdated", data.getLastUpdated());
                            usermap.put("device_token", instanceIdResult.getToken());
                            usermap.put("token_type", "android");
                            mDatabase.getRef().child("users").updateChildren(usermap);

                            if (utils.isNotEmptyString("" + otp)) {
                                if (isVerifyClicked) {
                                    verifyOTP(otp);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addUserInFirebase(UserDetails data) {
        try {
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                @Override
                public void onSuccess(InstanceIdResult instanceIdResult) {
                    try {
                        Log.e("device token", instanceIdResult.getToken());
                        if (instanceIdResult.getToken() != null) {
                            User_FB userFb = new User_FB(data.getId(), data.getFirstName(), data.getLastName(), data.getFullName(), data.getUsername(),
                                    data.getPhone(), data.getAbout(), data.getAvatar(), data.getLastUpdated(), instanceIdResult.getToken(), "android");

                            mDatabase.child("users").child("" + data.getId()).setValue(userFb);

                            if (utils.isNotEmptyString("" + otp)) {
                                if (isVerifyClicked) {
                                    verifyOTP(otp);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
