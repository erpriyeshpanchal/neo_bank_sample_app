package com.digiapp.sampleapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.digiapp.sampleapp.databinding.ActivityMainSampleBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import in.digiapp.waas.CustomerAppsSdkStatus;
import in.digiapp.waas.StartCustomerAppsSdk;


public class SampleMainActivity extends AppCompatActivity {

    private String API_KEY = "4051c9d9d616Jvuy0B3Y817t4ow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sample);
        ActivityMainSampleBinding binding =
                DataBindingUtil.setContentView(
                        this,
                        R.layout.activity_main_sample
                );

        binding.btnOpenNbSdk.setOnClickListener(view -> startNeoBankSdk(binding.etMobile.getText().toString()));
    }

    private void startNeoBankSdk(String mobileNum) {
        registerCustomerAppSdk();
        StartCustomerAppsSdk.launch(this,
                mobileNum,
                API_KEY,
                StartCustomerAppsSdk.WaasEnvironment.QA,
                getAppSignatures().get(0));
    }

    private void registerCustomerAppSdk() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCustomerAppsSdkStatusUpdate(CustomerAppsSdkStatus customerAppsSdkStatus) {
        Toast.makeText(this, customerAppsSdkStatus.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public ArrayList<String> getAppSignatures() {
        ArrayList<String> appCodes = new ArrayList<>();

        try {
            // Get all package signatures for the current package
            String packageName = getPackageName();
            PackageManager packageManager = getPackageManager();
            Signature[] signatures = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;

            // For each signature create a compatible hash
            for (Signature signature : signatures) {
                String hash = hash(packageName, signature.toCharsString());
                if (hash != null) {
                    appCodes.add(String.format("%s", hash));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            android.util.Log.e("TAG", "Unable to find package to obtain hash.", e);
        }
        return appCodes;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static String hash(String packageName, String signature) {
        String appInfo = packageName + " " + signature;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(BuildConfig.HASH_TYPE);
            messageDigest.update(appInfo.getBytes(StandardCharsets.UTF_8));
            byte[] hashSignature = messageDigest.digest();

            // truncated into NUM_HASHED_BYTES
            hashSignature = Arrays.copyOfRange(hashSignature, 0, Integer.parseInt(BuildConfig.NUM_HASHED_BYTES));
            // encode into Base64
            String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
            base64Hash = base64Hash.substring(0, Integer.parseInt(BuildConfig.NUM_BASE64_CHAR));

            android.util.Log.e("TAG", String.format("pkg: %s -- hash: %s", packageName, base64Hash));
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            android.util.Log.e("TAG", "hash:NoSuchAlgorithm", e);
        }
        return null;
    }
}