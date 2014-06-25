package com.bk.checkcompavail;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.bk.computeravailcheck.R;

public class SplashActivity extends Activity {
	
	// Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // Instantiate a progress dialog
        // with custom theme
        pd = new ProgressDialog(this, R.style.BKTheme);
        pd.setCancelable(false);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Large);
        pd.show();

        new Handler().postDelayed(new Runnable() {
        	 
            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */
 
            @Override
            public void run() {
            		
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashActivity.this, MapActivity.class);
                startActivity(i);

                // Opening transition animation
        	    overridePendingTransition(R.animator.activity_open_translate,
                        R.animator.activity_close_scale);
        			
        		// close this activity
                finish();

                // Dismiss the progress dialog
                pd.dismiss();
        			
            }
            
        }, SPLASH_TIME_OUT);

    }
    
}
