package com.example.myapplication.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.adapters.WebRTCClient;
import org.webrtc.SurfaceViewRenderer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapters.WebRTCClient;
import org.webrtc.SurfaceViewRenderer;

public class CallActivity extends AppCompatActivity {
    private WebRTCClient webRTCClient;
    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnCamera;
    private ImageButton btnEndCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        localView = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        btnMute = findViewById(R.id.btn_mute);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnCamera = findViewById(R.id.btn_camera);
        btnEndCall = findViewById(R.id.btn_end_call);

        // Initialize WebRTC
        webRTCClient = new WebRTCClient(this, localView, remoteView);

        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.toggleMute();
                btnMute.setImageResource(webRTCClient.isMuted() ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
            }
        });

        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement speaker functionality
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.toggleCamera();
                btnCamera.setImageResource(webRTCClient.isCameraEnabled() ? R.drawable.ic_videocam_on : R.drawable.ic_videocam_off);
            }
        });

        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.endCall();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up WebRTC resources
        webRTCClient.endCall();
    }
}
