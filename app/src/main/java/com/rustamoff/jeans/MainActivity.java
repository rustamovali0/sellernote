package com.rustamoff.jeans;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int MEDIA_PERMISSION_REQUEST = 1002;
    private static final int CAMERA_PERMISSION_REQUEST = 1003;
    private static final String APP_URL = "https://sellernote-xi.vercel.app/#sales";
    private static final long DOUBLE_BACK_EXIT_MS = 2000L;

    private WebView webView;
    private FrameLayout root;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;
    private long lastBackPressedAt = 0L;
    private boolean backCheckInProgress = false;
    private boolean openCameraAfterPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(246, 248, 252));
        getWindow().setNavigationBarColor(Color.rgb(246, 248, 252));

        // Android 15 daxil olmaqla bütün cihazlarda sistem barlarını özümüz nəzərə alırıq.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(246, 248, 252));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(246, 248, 252));
        webView.setClipToPadding(true);

        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(webView, webParams);
        setContentView(root);
        applyRealSafeSpacing();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAppSpacing();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> filePathCallbackValue,
                    FileChooserParams fileChooserParams) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePathCallbackValue;
                showImageSourceDialog();
                return true;
            }
        });

        // Tətbiqə ilk girişdə kamera və şəkil icazələrini istə.
        requestStartupMediaPermissions();

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void applyRealSafeSpacing() {
        final int extraTop = dp(26);
        final int extraBottom = dp(34);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) webView.getLayoutParams();
            params.topMargin = topInset + extraTop;
            params.bottomMargin = bottomInset + extraBottom;
            webView.setLayoutParams(params);

            return insets;
        });
        root.requestApplyInsets();
    }

    private void injectAppSpacing() {
        String js = "(function(){" +
                "var id='rustamoff-apk-spacing';" +
                "var s=document.getElementById(id);" +
                "if(!s){s=document.createElement('style');s.id=id;document.head.appendChild(s);}" +
                "s.textContent='" +
                ".app{padding-top:22px!important;padding-bottom:170px!important;}" +
                ".bottom-nav{bottom:24px!important;}" +
                ".floating-add{bottom:108px!important;}" +
                ".modal-backdrop{padding-top:28px!important;padding-bottom:24px!important;}" +
                "';" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLegacyWritePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return true;
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStartupMediaPermissions() {
        List<String> permissions = new ArrayList<>();

        if (!hasCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasGalleryPermission()) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (!hasGalleryPermission()) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !hasLegacyWritePermission()) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), MEDIA_PERMISSION_REQUEST);
        }
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Şəkil əlavə et")
                .setItems(new CharSequence[]{"Kamera", "Qalereya"}, (dialog, which) -> {
                    if (which == 0) {
                        openCameraChoice();
                    } else {
                        launchGallery();
                    }
                })
                .setNegativeButton("Bağla", (dialog, which) -> cancelFileChooser())
                .setOnCancelListener(dialog -> cancelFileChooser())
                .show();
    }

    private void cancelFileChooser() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        cameraImageUri = null;
    }

    private void openCameraChoice() {
        if (hasCameraPermission()) {
            launchCamera();
            return;
        }

        openCameraAfterPermission = true;
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean granted = hasCameraPermission();
            if (openCameraAfterPermission && granted && filePathCallback != null) {
                openCameraAfterPermission = false;
                launchCamera();
            } else {
                openCameraAfterPermission = false;
                Toast.makeText(this, "Kamera icazəsi verilmədi.", Toast.LENGTH_SHORT).show();
                if (filePathCallback != null) {
                    showImageSourceDialog();
                }
            }
            return;
        }

        if (requestCode == MEDIA_PERMISSION_REQUEST) {
            if (!hasCameraPermission()) {
                Toast.makeText(this, "Kamera üçün icazə verilməyib.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchGallery() {
        try {
            Intent galleryIntent;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                galleryIntent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                galleryIntent.setType("image/*");
                galleryIntent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 5);
            } else {
                galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }

            startActivityForResult(galleryIntent, FILE_CHOOSER_REQUEST);
        } catch (Exception error) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(fallback, FILE_CHOOSER_REQUEST);
            } catch (Exception secondError) {
                Toast.makeText(this, "Qalereya açıla bilmədi.", Toast.LENGTH_SHORT).show();
                cancelFileChooser();
            }
        }
    }

    private void launchCamera() {
        try {
            Intent cameraIntent = createCameraIntent();
            if (cameraIntent != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, FILE_CHOOSER_REQUEST);
            } else {
                Toast.makeText(this, "Kamera tətbiqi tapılmadı.", Toast.LENGTH_SHORT).show();
                showImageSourceDialog();
            }
        } catch (Exception error) {
            Toast.makeText(this, "Kamera açıla bilmədi.", Toast.LENGTH_SHORT).show();
            showImageSourceDialog();
        }
    }

    private Intent createCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = null;

        try {
            String fileName = "rustamoff_" + System.currentTimeMillis() + ".jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Rustamoff Jeans");
            }

            cameraImageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (cameraImageUri != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (Exception ignored) {
            cameraImageUri = null;
        }

        return cameraIntent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data != null) {
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    int count = Math.min(clipData.getItemCount(), 5);
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = clipData.getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            if (results == null && cameraImageUri != null) {
                results = new Uri[]{cameraImageUri};
            }
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
        cameraImageUri = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView == null || backCheckInProgress) return;

        backCheckInProgress = true;
        String script = "(function(){try{" +
                "if(typeof closeTopLayerForBack==='function'){return !!closeTopLayerForBack();}" +
                "var lb=document.getElementById('imageLightbox');" +
                "if(lb&&!lb.classList.contains('hidden')){lb.classList.add('hidden');return true;}" +
                "var m=document.querySelector('.modal-backdrop:not(.hidden)');" +
                "if(m){m.classList.add('hidden');return true;}" +
                "return false;}catch(e){return false;}})();";

        webView.evaluateJavascript(script, result -> {
            backCheckInProgress = false;
            if ("true".equals(result)) {
                lastBackPressedAt = 0L;
                return;
            }

            if (webView.canGoBack()) {
                webView.goBack();
                lastBackPressedAt = 0L;
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastBackPressedAt <= DOUBLE_BACK_EXIT_MS) {
                finish();
            } else {
                lastBackPressedAt = now;
                Toast.makeText(this, "Tətbiqdən çıxmaq üçün geri düyməsinə iki dəfə basın", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
