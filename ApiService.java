package com.radio.chat.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.radio.chat.rest.model.blocklistmodel.Blocklistmain;
import com.radio.chat.rest.model.commentmodel.CommentMainmodel;
import com.radio.chat.rest.model.otpmodel.OtpResponse;
import com.radio.chat.rest.model.otpmodel.VerifyResponse;
import com.radio.chat.rest.model.postCommentmodel.PostCmtmodel;
import com.radio.chat.rest.model.synccontactmodel.SyncContactModel;
import com.radio.chat.rest.model.userdetailsmodel.UserAvilablity;
import com.radio.chat.rest.model.userdetailsmodel.UserDetailsResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    String token = "";

  /*
    @POST("getCountryList")
    Call<ApiResponse> calledGetCountryList(@Body JsonObject jsonObject);*/

    @GET("otp/generate/{phone}")
    Call<OtpResponse> generateOTP(@Path("phone") String phone, @Query("otp") boolean otp);

    @FormUrlEncoded
    @POST("otp/verify/{phone}")
    Call<VerifyResponse> verifyOTP(@Path("phone") String phone,
                                   @Field("phone") String phone_num,
                                   @Field("otp") int otp,
                                   @Field("time") String time);

    @POST("auth/user")
    Call<UserDetailsResponse> getUserDetails(@Header("Authorization") String token);

    @Headers("Content-Type: application/json")
    @POST("user/update")
    Call<UserDetailsResponse> getUpdatedUserDetails(@Header("Authorization") String token, @Body JsonObject data);

    @POST("user/username-availability")
    Call<UserAvilablity> checkUsernameavailable(@Header("Authorization") String token, @Body JsonObject input);

    @POST("contacts/sync")
    Call<SyncContactModel> syncContacts(@Header("Authorization") String auth, @Body JsonObject jsonObject);

    @GET("contacts")
    Call<SyncContactModel> getContacts(@Header("Authorization") String auth, @Query("user") String userid);

    @GET("posts/")
    Call<JsonObject> getPosts(@Header("Authorization") String auth, @Query("page") int page);

    @GET("posts/like-dislike/{post_id}")
    Call<JsonObject> likePost(@Header("Authorization") String auth, @Path("post_id") String post_id);

    @POST("posts/comment/{post_id}")
    Call<CommentMainmodel> commentPost(@Header("Authorization") String auth, @Path("post_id") String post_id, @Body JsonObject jsonObject);

    @GET("posts/delete-comment/{post_id}/{comment_id}")
    Call<JsonObject> deleteComment(@Header("Authorization") String auth, @Path("post_id") String post_id, @Path("comment_id") String comment_id,@Query("comment") String comment);

    @POST("posts/create")
    Call<JsonObject> createPost(@Header("Authorization") String token, @Body JsonObject data);

    @POST("posts/delete/{post_id}")
    Call<JsonObject> deletePost(@Header("Authorization") String token, @Path("post_id")String post_id );

    @GET("posts?user-post=true")
    Call<JsonObject> getUserPosts(@Header("Authorization") String auth, @Query("page") int page);

    @POST("user/delete-account")
    Call<JsonObject> deleteUserAccount(@Header("Authorization") String auth);

    @POST("user/last-seen")
    Call<JsonObject> lastSeenUpdate(@Header("Authorization") String auth);

    @GET("posts/comments/{postId}")
    Call<PostCmtmodel> getComments(@Header("Authorization") String auth, @Path("postId") String post_id);

    @GET("block-list")
    Call<JsonArray> getBlocklist(@Header("Authorization") String auth);

    @POST("block-list/block")
    Call<Blocklistmain> blockUser(@Header("Authorization") String auth, @Body JsonObject id);

    @POST("block-list/unblock/{blocklist}")
    Call<Blocklistmain> unblockUser(@Header("Authorization") String auth, @Path("blocklist") String user_id );
}

