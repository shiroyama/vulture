package us.shiroyama.android.vulture.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.List;

import us.shiroyama.android.vulture.annotations.ObserveLifecycle;
import us.shiroyama.android.vulture.annotations.SafeCallback;
import us.shiroyama.android.vulture.annotations.Serializable;
import us.shiroyama.android.vulture.sample.data.User;
import us.shiroyama.android.vulture.sample.serializers.ParcelSerializer;

/**
 * @author Fumihiko Shiroyama
 */

@ObserveLifecycle
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchAsynchronously();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        SafeMainActivity.register(this);
    }

    @Override
    protected void onPause() {
        SafeMainActivity.unregister();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState called.");
    }

    private void fetchAsynchronously() {
        Log.d(TAG, "fetchAsynchronously(): start.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Do asynchronous job here
                    Thread.sleep(2000L);
                    Log.d(TAG, "fetchAsynchronously(): finished.");

                    String resultMessage = "ASYNCHRONOUS JOB DONE!!!";

                    // calling this this directly may cause crashing after onSaveInstanceState
                    // doCallback(resultMessage);

                    // call this instead.
                    SafeMainActivity.doCallbackSafely(resultMessage);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }).start();
    }

    private void fetchUsers() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "fetchUsers(): started.");
                    Thread.sleep(2000L);
                    Log.d(TAG, "fetchUsers(): finished.");

                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }).start();
    }

    @SafeCallback
    void doCallback(@NonNull String message) {
        Log.d(TAG, "doCallback() called. message: " + message);
        FinishDialog.newInstance(message).show(getSupportFragmentManager(), FinishDialog.TAG);
    }

    @SafeCallback
    void fetchUsersCallback(@Serializable(serializer = ParcelSerializer.class) List<User> users) {
        Log.d(TAG, "fetchUsersCallback() is callbacked.");
        for (User user : users) {
            Log.d(TAG, "user name: " + user.getName());
            Log.d(TAG, "user age: " + user.getAge());
        }
    }

}
