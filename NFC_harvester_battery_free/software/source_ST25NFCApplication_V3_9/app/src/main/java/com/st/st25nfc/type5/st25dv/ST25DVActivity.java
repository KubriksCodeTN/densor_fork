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

package com.st.st25nfc.type5.st25dv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.densor.DensorCommon;
import com.st.st25nfc.densor.data.DensorDataSet;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25dv.ST25DVTag;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.SlidingTabLayout;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STPagerAdapter;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.generic.util.UIHelper.STFragmentId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ST25DVActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, STFragment.STFragmentListener {

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    final static String TAG = "ST25DVActivity";
    public ST25DVTag mST25DVTag;

    STPagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private SlidingTabLayout mSlidingTabLayout;

    ListView lv;

    ImageView nfcStatusImage;
    TextView nfcStatusText;

    Handler handler;

    public Thread connectionCheckThread;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_layout_with_tag_status);

        if (super.getTag() instanceof ST25DVTag) {
            mST25DVTag = (ST25DVTag) super.getTag();
        }
        if (mST25DVTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Densor");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mMenu = ST25Menu.newInstance(super.getTag());
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        List<STFragmentId> fragmentList = new ArrayList<STFragmentId>();

        // Included Densor screens.
        fragmentList.add(UIHelper.STFragmentId.TAG_INFO_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.DENSOR_TIMESYNC_ID);
        fragmentList.add(UIHelper.STFragmentId.DENSOR_SETTINGS_ID);
        fragmentList.add(UIHelper.STFragmentId.RAW_DATA_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.SYS_FILE_TYP5_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.DENSOR_DATA_VIEW);

        mPagerAdapter = new STPagerAdapter(getSupportFragmentManager(), getApplicationContext(), fragmentList);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

        // Check if the activity was started with a request to select a specific tab
        Intent mIntent = getIntent();
        int tabNbr = mIntent.getIntExtra("select_tab", -1);
        if(tabNbr != -1) {
            mViewPager.setCurrentItem(tabNbr);
        }

        handler = new Handler();

        nfcStatusImage = (ImageView) findViewById(R.id.nfc_status_image);
        nfcStatusText = (TextView) findViewById(R.id.nfc_status_text);

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        finish();
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

    void processIntent(Intent intent) {
        Log.d(TAG, "Process Intent");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return mMenu.selectItem(this, item);
    }

    @Override
    public void onPause() {
        super.onPause();
        connectionCheckThread.interrupt();
    }

    @Override
    public void onResume() {
        super.onResume();
        connectionCheckThread = new Thread(new ConnectionCheckAction());
        connectionCheckThread.start();
    }

    public ST25DVTag getTag() {
        return mST25DVTag;
    }

    /**
     * Task to check the connection to a Densor every second. Will either report connected (memory pointer if running, charge level when in charge mode) or not connected.
     */
    class ConnectionCheckAction implements Runnable {
        private boolean tagInField = false;

        public void run() {
            NFCTag tag = getTag();

            while (true) {
                if (tag != null && handler != null) {
                    Log.i("ConnectionCheck", "Still running!");
                    try {
                        // Read the two MSBs of the start time registers.
                        byte[] tsBuff = tag.readBytes(DensorCommon.timeRegister, 2);
                        float ts = (float) ((((int) (tsBuff[0] & 0xff)) << 8) | (tsBuff[1] & 0xff)) / 1000;;

                        // Read the memory pointer.
                        byte [] mempointerBuff = tag.readBytes(DensorCommon.memoryPointerRegister,2);
                        int mp = (int) ((((int) (mempointerBuff[1] & 0xff)) << 8) + (mempointerBuff[0] & 0xff)) ;

                        // No exception thrown, Densor is connected.
                        Log.i("ConnectionCheck", "NFC tag in field");

                        // If the two MSBs of the time start register are bigger then or equal to 3.4, the Densor is running. Report the memory pointer.
                        if (ts >= 3.4 && !tagInField) {
                            tagInField = true;
                            handler.post(new Runnable() {
                                public void run() {
                                    if (mp == 8192){
                                        nfcStatusText.setText(String.format("Densor running. Possible Glitch %s",mp));
                                    }
                                    else {
                                        nfcStatusText.setText(String.format("Densor running. Bytes written: %s", mp));
                                    }
                                    nfcStatusImage.setImageResource(android.R.drawable.presence_online);
                                }
                            });
                        // The two MSBs of the time start register are smaller then 3.4, the Densor is in charge mode. Report charge level.
                        } else if (ts < 3.4) {
                            tagInField = true;
                            handler.post(new Runnable() {
                                public void run() {
                                    nfcStatusText.setText(String.format("NFC tag in range! Charging status: %s V", ts));
                                    nfcStatusImage.setImageResource(android.R.drawable.presence_online);
                                }
                            });
                        }

                    } catch (STException e) {
                        // Connection failed. Report not connected.
                        if (tagInField) {
                            tagInField = false;
                            handler.post(new Runnable() {
                                public void run() {
                                    nfcStatusText.setText(R.string.nfc_tag_is_not_in_range);
                                    nfcStatusImage.setImageResource(android.R.drawable.presence_offline);
                                }
                            });
                        }
                        Log.w("ConnectionCheck", "NFC tag not in field");
                    }
                    try {
                        // Sleep for a second and check again.
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }
}


