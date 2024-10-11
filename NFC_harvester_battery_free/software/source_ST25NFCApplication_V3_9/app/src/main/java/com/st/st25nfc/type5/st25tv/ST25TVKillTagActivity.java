/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
  *
  * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
  * You may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  *        http://www.st.com/Mix_MyLiberty
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
  * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  ******************************************************************************
*/

package com.st.st25nfc.type5.st25tv;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.command.Iso15693CustomKillCommandInterface;
import com.st.st25sdk.type5.st25tv.ST25TVTag;

import static com.st.st25nfc.type5.st25tv.ST25TVKillTagActivity.Action.ENTER_CURRENT_KILL_PWD;
import static com.st.st25nfc.type5.st25tv.ST25TVKillTagActivity.Action.ENTER_NEW_KILL_PWD;
import static com.st.st25nfc.type5.st25tv.ST25TVKillTagActivity.Action.KILL_TAG;


public class ST25TVKillTagActivity extends STFragmentActivity implements STType5PwdDialogFragment.STType5PwdDialogListener, NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        KILL_TAG,
        ENTER_CURRENT_KILL_PWD,
        ENTER_NEW_KILL_PWD
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "KillActivity";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    int configurationPasswordNumber;
    int killPasswordNumber;
    private NFCTag mNFCTag;

    private Iso15693CustomKillCommandInterface mKillCommandInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tv_kill_tag, null);
        frameLayout.addView(childView);

        TextView warningTextView = findViewById(R.id.warningTextView);

        mNFCTag = MainActivity.getTag();
        if (mNFCTag instanceof ST25TVTag) {
            killPasswordNumber = ST25TVTag.ST25TV_KILL_PASSWORD_ID;
            configurationPasswordNumber = ST25TVTag.ST25TV_CONFIGURATION_PASSWORD_ID;
        } else {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        Button killButton = (Button) findViewById(R.id.killButton);
        killButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killTag();
            }
        });

        Button changeKillPasswordButton = (Button) findViewById(R.id.changeKillPasswordButton);
        changeKillPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterNewKillPassword();
            }
        });

        Button lockKillPasswordButton = (Button) findViewById(R.id.lockKillPasswordButton);
        lockKillPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askConfirmation();
            }
        });

        // This Activity will fail if the tag doesn't implement a Iso15693CustomKillCommandInterface
        try {
            mKillCommandInterface = (Iso15693CustomKillCommandInterface) MainActivity.getTag();
        } catch (ClassCastException e) {
            Log.e(TAG, "Error! Tag not implementing Iso15693CustomKillCommandInterface!");
            return;
        }

    }

    private void killTag() {
        mCurrentAction = KILL_TAG;
        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.KILL_TAG,
                killPasswordNumber,
                getResources().getString(R.string.kill_warning));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
    }

    private void enterNewKillPassword() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "enterNewKillPassword");

                mCurrentAction = ENTER_NEW_KILL_PWD;
                int passwordNumber = killPasswordNumber;
                String message = getResources().getString(R.string.enter_new_kill_pwd);
                STType5PwdDialogFragment.STPwdAction action = STType5PwdDialogFragment.STPwdAction.ENTER_NEW_KILL_PWD;

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(action, passwordNumber, message);
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        }).start();
    }

    private void askConfirmation() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.lock_kill_pwd_question))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.lock_password),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        lockKillPassword();
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void lockKillPassword() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "lockKillPassword");

                try {
                    mKillCommandInterface.lockKill();
                    showToast(R.string.command_successful);
                } catch (STException e) {
                    e.printStackTrace();
                    showToast(R.string.command_failed);
                }

            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(toolbar_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long

        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }

    private void presentCurrentKillPassword() {
        Log.v(TAG, "presentCurrentKillPassword");

        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                mCurrentAction = ENTER_CURRENT_KILL_PWD;
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        killPasswordNumber,
                        getResources().getString(R.string.enter_kill_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
    }

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        switch(mCurrentAction) {
            case ENTER_CURRENT_KILL_PWD:
                if (result == PwdDialogFragment.RESULT_OK) {
                    enterNewKillPassword();
                }
                break;

            default:
                break;
        }

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }
}
