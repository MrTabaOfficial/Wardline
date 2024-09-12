package com.example.myapplication.adapters;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.content.Context;
import android.util.Log;

import org.webrtc.*;

public class WebRTCClient {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private VideoTrack remoteVideoTrack;
    private VideoCapturer videoCapturer;
    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;
    private AudioTrack localAudioTrack;

    private boolean isMuted = false;
    private boolean isCameraEnabled = true;

    public WebRTCClient(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView) {
        this.localView = localView;
        this.remoteView = remoteView;
        initializePeerConnectionFactory(context);
        createVideoTrackFromCameraAndShowIt(context);
    }

    private void initializePeerConnectionFactory(Context context) {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(null, true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(null);

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    private void createVideoTrackFromCameraAndShowIt(Context context) {
        videoCapturer = createVideoCapturer(context);
        if (videoCapturer != null) {
            VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
            localAudioTrack = peerConnectionFactory.createAudioTrack("101", peerConnectionFactory.createAudioSource(new MediaConstraints()));

            EglBase eglBase = EglBase.create();  // Create an EglBase instance
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            localView.init(eglBase.getEglBaseContext(), null);
            localView.setMirror(true);
            localVideoTrack.addSink(localView);
            videoCapturer.startCapture(1024, 720, 30);
        } else {
            // Handle error: Unable to create video capturer
            Log.e(TAG, "Failed to create video capturer");
        }
    }





    private VideoCapturer createVideoCapturer(Context context) {
        VideoCapturer videoCapturer;
        if (Camera2Enumerator.isSupported(context)) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    public void toggleMute() {
        isMuted = !isMuted;
        localAudioTrack.setEnabled(!isMuted);
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void toggleCamera() {
        isCameraEnabled = !isCameraEnabled;
        localVideoTrack.setEnabled(isCameraEnabled);
    }

    public boolean isCameraEnabled() {
        return isCameraEnabled;
    }

    public void endCall() {
        // Stop the video capturer
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping video capturer: " + e.getMessage());
            }
            videoCapturer = null;
        }

        // Release local tracks
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }

        // Release peer connection
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
    }
}
