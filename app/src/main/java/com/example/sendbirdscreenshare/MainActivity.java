package com.example.sendbirdscreenshare;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.sendbird.calls.AcceptParams;
import com.sendbird.calls.AuthenticateParams;
import com.sendbird.calls.CallOptions;
import com.sendbird.calls.DialParams;
import com.sendbird.calls.DirectCall;
import com.sendbird.calls.SendBirdCall;
import com.sendbird.calls.SendBirdException;
import com.sendbird.calls.SendBirdVideoView;
import com.sendbird.calls.User;
import com.sendbird.calls.handler.AuthenticateHandler;
import com.sendbird.calls.handler.CompletionHandler;
import com.sendbird.calls.handler.DialHandler;
import com.sendbird.calls.handler.DirectCallListener;
import com.sendbird.calls.handler.SendBirdCallListener;

import org.jetbrains.annotations.Nullable;
import org.webrtc.RendererCommon;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    /**
     * User your confguration here
     */
    String APP_ID = "YOUR SENDBIRD APPLICATION ID HERE";
    String USER_ID = "ANY OF YOUR SENDBIRD USER ID HERE";
    String ACCESS_TOKEN = null;

    /**
     * User you want to call to
     */
    String CALLEE_ID = "789298";

    /**
     * Other parameters here
     */
    Context mContext = this;
    String UNIQUE_HANDLER_ID = "USER ID TO CALL TO";

    /**
     * Current active call
     */
    DirectCall mCurentCall;

    /**
     * Log tag
     */
    public static final String TAG = "Sendbird Screen share";

    /**
     * Elements on screen
     */
    Button butConnect;
    Button butMakeCall;
    Button butEndCall;
    Button butShareScreen;
    Button butStopShareScreen;
    SendBirdVideoView mVideoViewFullScreen;
    SendBirdVideoView mVideoViewSmall;

    /**
     * Check camera permission
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final String[] MANDATORY_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Share screen permission
     */
    private static final int SCREEN_CAPTURE_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Init elements on screen
        initElementsOnScreen();
    }

    /**
     * Init elements on screen and
     * set listeners
     */
    private void initElementsOnScreen() {
        // Connect to Sendbird Calls
        butConnect = (Button) findViewById(R.id.butConnect);
        butConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Run this method when this button is clicked
                connect();
            }
        });
        // Make call button
        butMakeCall = (Button) findViewById(R.id.butMakeCall);
        butMakeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Run this method when this button is clicked
                makeCall();
            }
        });
        // End call button
        butEndCall = (Button) findViewById(R.id.butEndCall);
        butEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
        // Share screen button
        butShareScreen = (Button) findViewById(R.id.butShareScreen);
        butShareScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Run this method when this button is clicked
                startScreenShare();
            }
        });
        // Stop screen share button
        butStopShareScreen = (Button) findViewById(R.id.butStopShareScreen);
        butStopShareScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenShare();
            }
        });
        // Video (remove)
        mVideoViewFullScreen = findViewById(R.id.video_view_fullscreen);
        mVideoViewFullScreen.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mVideoViewFullScreen.setZOrderMediaOverlay(false);
        mVideoViewFullScreen.setEnableHardwareScaler(true);
        // Video (my video)
        mVideoViewSmall = findViewById(R.id.video_view_small);
        mVideoViewSmall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mVideoViewSmall.setZOrderMediaOverlay(true);
        mVideoViewSmall.setEnableHardwareScaler(true);
    }

    /**
     * Connect to Sendbird
     */
    private void connect() {
        // Initialize SendBirdCall
        SendBirdCall.init(getApplicationContext(), APP_ID);
        // Authenticate user
        authenticateUser();
    }

    /**
     * Authenticate user with Sendbird
     */
    private void authenticateUser() {
        // The USER_ID below should be
        // unique to your Sendbird application.
        AuthenticateParams params =
                new AuthenticateParams(USER_ID)
                .setAccessToken(ACCESS_TOKEN);
        // Authenticate user
        SendBirdCall.authenticate(params, new AuthenticateHandler() {
            @Override
            public void onResult(User user, SendBirdException e) {
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(mContext,
                            "Error authenticating user.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Log.i(TAG, "User connected");
                    // Hide the connect button
                    butConnect.setVisibility(View.GONE);
                    // Show the make call button
                    butMakeCall.setVisibility(View.VISIBLE);
                    waitForCalls();
                    checkPermissions();
                }
            }
        });
    }

    /**
     * Wait for remote calls
     */
    private void waitForCalls() {
        SendBirdCall.removeAllListeners();
        SendBirdCall.addListener(UNIQUE_HANDLER_ID, new SendBirdCallListener() {
            @Override
            public void onRinging(DirectCall call) {
                call.setListener(new DirectCallListener() {
                    @Override
                    public void onEstablished(DirectCall call) {
                        Log.i(TAG, "Call established");
                        mCurentCall = call;
                        // Start to show video
                        setVideoOnceCallIsConnected(call);
                        // Hide Make Call button
                        butMakeCall.setVisibility(View.GONE);
                        // Show the End Call button
                        butEndCall.setVisibility(View.VISIBLE);
                        // Show Screen share button
                        butShareScreen.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onConnected(DirectCall call) {
                        Log.i(TAG, "Call connected to the other peer");
                        mCurentCall = call;
                    }
                    @Override
                    public void onEnded(DirectCall call) {
                        Log.i(TAG, "Call ended");
                        mCurentCall = null;
                        // Hide view view
                        mVideoViewSmall.setVisibility(View.GONE);
                        mVideoViewFullScreen.setVisibility(View.GONE);
                        // Show Make call button
                        butMakeCall.setVisibility(View.VISIBLE);
                        // Hide End call button
                        butEndCall.setVisibility(View.GONE);
                        // Hide Screen share button
                        butShareScreen.setVisibility(View.GONE);
                    }
                    @Override
                    public void onRemoteAudioSettingsChanged(DirectCall call) {
                        Log.i(TAG, "Remote audio settings changed");
                    }
                });
                CallOptions callOptions = new CallOptions()
                        .setLocalVideoView(mVideoViewSmall)
                        .setRemoteVideoView(mVideoViewFullScreen)
                        .setVideoEnabled(true)
                        .setAudioEnabled(true);
                call.accept(new AcceptParams().setCallOptions(callOptions));
            }
        });
        /** You can define your own sounds for your calls
         *
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.DIALING, R.raw.dialing);
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RINGING, R.raw.ringing);
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RECONNECTING, R.raw.reconnecting);
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RECONNECTED, R.raw.reconnected);
         */
        firebaseToken();
    }

    private void firebaseToken() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(
                MainActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                Log.i("FCM Token", token);
                SendBirdCall.registerPushToken(token, false, e -> {
                    if (e != null) {
                        Log.i(MainActivity.TAG,
                                "[PushUtils] registerPushToken() => e: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * You need to check if permissions are given
     * for accessing your camera
     */
    private void checkPermissions() {
        ArrayList<String> deniedPermissions = new ArrayList<>();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
            }
        }
        if (deniedPermissions.size() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(deniedPermissions.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
            } else {
                android.util.Log.e("VideoChat", "[VideoChatActivity] PERMISSION_DENIED");
            }
        }
    }

    /**
     * Make a call to a remote Sendbird user
     */
    private void makeCall() {
        // Call options
        CallOptions callOptions = new CallOptions()
            .setLocalVideoView(mVideoViewSmall)
            .setRemoteVideoView(mVideoViewFullScreen)
            .setVideoEnabled(true)
            .setAudioEnabled(true);
        // Set parameters
        DialParams params = new DialParams(CALLEE_ID);
        params.setVideoCall(true);
        params.setCallOptions(callOptions);
        // Make the call
        DirectCall call = SendBirdCall.dial(params, new DialHandler() {
            @Override
            public void onResult(DirectCall call, SendBirdException e) {
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(mContext,
                            "Error dialing.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        call.setListener(new DirectCallListener() {
            @Override
            public void onEstablished(DirectCall call) {
                mCurentCall = call;
                // Start to show video
                setVideoOnceCallIsConnected(call);
                // Hide Make call button
                butMakeCall.setVisibility(View.GONE);
                // Show End call button
                butEndCall.setVisibility(View.VISIBLE);
                // Show Screen share button
                butShareScreen.setVisibility(View.VISIBLE);
            }
            @Override
            public void onConnected(DirectCall call) {
                Log.i(TAG, "Call connected");
            }
            @Override
            public void onEnded(DirectCall call) {
                Log.i(TAG, "Call ended");
                // Hide view view
                mVideoViewSmall.setVisibility(View.GONE);
                mVideoViewFullScreen.setVisibility(View.GONE);
                // Show Make call button
                butMakeCall.setVisibility(View.VISIBLE);
                // Hide End call button
                butEndCall.setVisibility(View.GONE);
                // Hide Screen share button
                butShareScreen.setVisibility(View.GONE);
            }
        });
    }

    private void setVideoOnceCallIsConnected(DirectCall call) {
        Log.i(TAG, "Initializing video elements...");
        mVideoViewSmall.setVisibility(View.VISIBLE);
        mVideoViewFullScreen.setVisibility(View.VISIBLE);
        call.setRemoteVideoView(mVideoViewFullScreen);
        call.setLocalVideoView(mVideoViewSmall);
    }

    /**
     * End current call
     */
    private void endCall() {
        if (mCurentCall == null) {
            return;
        }
        // Hide video view
        mVideoViewSmall.setVisibility(View.GONE);
        mVideoViewFullScreen.setVisibility(View.GONE);
        // End the call
        mCurentCall.end();
        // Show Make call button
        butMakeCall.setVisibility(View.VISIBLE);
        // Hide End call button
        butEndCall.setVisibility(View.GONE);
        // Hide Screen share button
        butShareScreen.setVisibility(View.GONE);
    }


    /**
     * SHARE SCREEN FUNCTIONS
     */

    @TargetApi(21)
    private void startScreenShare() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopScreenShare() {
        if (mCurentCall == null) {
            return;
        }
        mCurentCall.stopScreenShare(new CompletionHandler() {
            @Override
            public void onResult(@Nullable SendBirdException e) {
                // Hide stop screen share button
                butStopShareScreen.setVisibility(View.GONE);
                // Show Share screen button
                butShareScreen.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCREEN_CAPTURE_PERMISSION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Screen capture permission request done");
                shareMyScreenAfterAcceptingPermission(intent);
            }
        }
    }

    private void shareMyScreenAfterAcceptingPermission(Intent screenCaptureIntent) {
        if (mCurentCall == null) {
            return;
        }
        mCurentCall.startScreenShare(screenCaptureIntent, new CompletionHandler() {
            @Override
            public void onResult(@Nullable SendBirdException e) {
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(
                            mContext,
                            "Error starting screen share",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Toast.makeText(
                            mContext,
                            "Screen sharing in progress",
                            Toast.LENGTH_LONG
                    ).show();
                    // Show stop screen share button
                    butStopShareScreen.setVisibility(View.VISIBLE);
                    // Hide the share screen button
                    butShareScreen.setVisibility(View.GONE);
                }
            }
        });
    }
}