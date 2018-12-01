package com.project.chatflix.activity.Sinch_Calling;

import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.project.chatflix.R;
import com.project.chatflix.object.AudioPlayer;
import com.project.chatflix.service.SinchService;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CallScreenActivity extends BaseActivity {
    public static boolean isCalling = false;
    public boolean isEstablishing;
    //    public boolean quitCall;
    static final String TAG = CallScreenActivity.class.getSimpleName();

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;

    private String mCallId, mRoomId;
    private long mCallStart = 0;

    private TextView mCallDuration;
    private TextView mCallState;
    private TextView mCallerName;

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            CallScreenActivity.this.runOnUiThread(() -> {
                updateCallDuration();
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.callscreen);

        mAudioPlayer = new AudioPlayer(this);
        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        Button endCallButton = findViewById(R.id.hangupButton);

        endCallButton.setOnClickListener(v -> {
//                quitCall = true;
            endCall();
        });
        mCallStart = System.currentTimeMillis();
        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
        mRoomId = getIntent().getStringExtra("Room");

        isEstablishing = false;
//        quitCall = false;

    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());
            mCallerName.setText("Calling " + call.getRemoteUserId() + " ..");
            mCallState.setText(call.getState().toString());
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mDurationTask.cancel();
        mTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer();
        mDurationTask = new UpdateCallDurationTask();
        mTimer.schedule(mDurationTask, 0, 500);
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.

    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private String formatTimespan(long timespan) {
        long totalSeconds = timespan / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void updateCallDuration() {
        if (mCallStart > 0) {
            mCallDuration.setText(formatTimespan(System.currentTimeMillis() - mCallStart));
        }
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

            String endMsg = "Call ended: " + call.getDetails().toString();
//            Log.e("Call Detail", endMsg);
//            Toast.makeText(CallScreenActivity.this, endMsg, Toast.LENGTH_LONG).show();

            //Thêm Call Message vào Firebase

//            String current_user_ref = "message/" + mRoomId + "/" +
//                    String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());
//
//            String chat_user_ref = "message/" +
//                    String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())
//                    + "/" + mRoomId;
//
//            DatabaseReference user_message_push = FirebaseDatabase.getInstance().getReference()
//                    .child("message")
//                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
//                    .child(mRoomId)
//                    .push();
//
//            final String push_id = user_message_push.getKey();

//            Map messageUserMap = new HashMap();
//            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
//            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

//            FirebaseDatabase.getInstance().getReference()
//                    .updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if(databaseError != null) {
//
//                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
//
//                            }
//                        }
//                    });

            isCalling = true;

            //Kiểm tra nếu gọi đc mới lưu
            if (isEstablishing) {

                // Lưu thông tin cuộc gọi để gửi lên Firebase
                Map messageMap = new HashMap();
                messageMap.put("text", "Incoming Call....");
                messageMap.put("idSender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                messageMap.put("idReceiver", mRoomId);
                messageMap.put("type", "call");
                messageMap.put("duration", formatTimespan(System.currentTimeMillis() - mCallStart));
                messageMap.put("timestamp", ServerValue.TIMESTAMP);

                FirebaseDatabase.getInstance().getReference().child(getString(R.string.message_table) + "/" + mRoomId)
                        .push().setValue(messageMap);
            }
            //Chưa gọi đc thì báo gọi nhỡ
            else {
                // Lưu thông tin cuộc gọi để gửi lên Firebase
                Map messageMap = new HashMap();
                messageMap.put("text", "Missed call");
                messageMap.put("idSender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                messageMap.put("idReceiver", mRoomId);
                messageMap.put("type", "call");
                messageMap.put("duration", "Missed call");
                messageMap.put("timestamp", ServerValue.TIMESTAMP);

                FirebaseDatabase.getInstance().getReference().child(getString(R.string.message_table) + "/" + mRoomId)
                        .push().setValue(messageMap);
            }

            // Kết thúc cuộc gọi
            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            isEstablishing = true;
            mAudioPlayer.stopProgressTone();
            mCallState.setText(call.getState().toString());
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            mCallStart = System.currentTimeMillis();
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
            isEstablishing = false;
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }

    }
}
