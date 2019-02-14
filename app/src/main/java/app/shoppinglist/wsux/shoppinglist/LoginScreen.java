package app.shoppinglist.wsux.shoppinglist;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Inflater;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;


public class LoginScreen extends Fragment implements View.OnClickListener {

    private static final int[] BACKGROUND_IMAGES = {
            R.drawable.login,
            R.drawable.login1,
            R.drawable.login2
    };

    private View loginScreenLayout;
    private ViewPager backgroundSlider;
    private Handler handler;
    private Timer swipeTimer;

    public LoginScreen() {}
    private FireBaseManager fireBaseManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        loginScreenLayout = inflater.inflate(R.layout.fragment_login_screen, container, false);
        loginScreenLayout.findViewById(R.id.sign_in_button).setOnClickListener(this);
        setSlider(loginScreenLayout);

        return loginScreenLayout;
    }

    public void show() {
        loginScreenLayout.setVisibility(View.VISIBLE);
        startSlider();
    }

    public void hide() {
        loginScreenLayout.setVisibility(View.GONE);
        stopSlider();
    }

    public void setFirebaseManager(FireBaseManager firebaseManager) {
        this.fireBaseManager = firebaseManager;
    }

    private void setSlider(View mainView) {
        backgroundSlider = mainView.findViewById(R.id.background_slider);
        backgroundSlider.setAdapter(new SliderAdapter());

        this.handler = new Handler();
    }

    private void slideNext() {
        int currentPage = backgroundSlider.getCurrentItem();
        if (currentPage >= BACKGROUND_IMAGES.length) {
            currentPage = 0;
            backgroundSlider.setCurrentItem(currentPage, false);
        }
        backgroundSlider.setCurrentItem(currentPage + 1, true);
    }

    private void startSlider() {
        swipeTimer = new Timer();
        swipeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        slideNext();
                    }
                });
            }
        }, 5000, 5000);
    }

    private void stopSlider() {
        if (swipeTimer == null) {
            return;
        }
        swipeTimer.cancel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInButtonClick();
                break;
        }
    }


    private void onSignInButtonClick() {
        fireBaseManager.getLoginManager().requestLogin();
    }

    private class SliderAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return BACKGROUND_IMAGES.length + 1;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.slider_image_item, null, false);

            if (position >= BACKGROUND_IMAGES.length) {
                position = 0;
            }

            ImageView imageView = view.findViewById(R.id.slider_item_image);
            imageView.setImageResource(BACKGROUND_IMAGES[position]);
            container.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view.equals(o);
        }
    }
}
