/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2019 STMicroelectronics</center></h2>
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

package com.st.st25nfc.type5.st25tvc;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.util.DisplayTapTagRequest;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static com.st.st25nfc.type5.st25tvc.ST25TVCChangeMemConf.ActionStatus.*;


public class ST25TVCChangeMemConf extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener,
        STType5PwdDialogFragment.STType5PwdDialogListener {

    private final int TOUCH_RANGE = 50;

    static final String TAG = "TVANDEFChangeMemConf";
    private ST25TVCTag mST25TVCTag;
    FragmentManager mFragmentManager;

    private RelativeLayout mColumn2Layout;
    private int mWholeTagHeightInPixels;        // Height of the graphical representation of the tag
    private int mTotalMemorySizeInBlocks;       // Including ANDEF + TruST25 area

    private TextView mArea1TextView;
    private TextView mArea2TextView;
    private TextView mAndefAreaTextView;

    private boolean mAreAreaSizesKnown = false;


    // WARNING: EndOfArea values should be stored in a byte but we use an int to simplify the
    //          calculations and comparisons.
    private int mEndOfArea1;

    private boolean mIsArea1ConfigurationLocked;

    private TextView mArea1SizeInBytesTextView;
    private TextView mArea1SizeInBlocksTextView;

    private TextView mArea2SizeInBytesTextView;
    private TextView mArea2SizeInBlocksTextView;

    private TextView mAndefAreaSizeInBytesTextView;
    private TextView mAndefAreaSizeInBlocksTextView;

    private TextView mTotalSizeInBytesTextView;
    private TextView mTotalSizeInBlocksTextView;

    enum Action {
        READ_REGISTER_VALUES,
        WRITE_REGISTER_VALUES
    }

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        AREA1_CONFIGURATION_IS_LOCKED
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.st25tvc_memory_configuration, null);
        frameLayout.addView(childView);

        mST25TVCTag = (ST25TVCTag) MainActivity.getTag();
        if (mST25TVCTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mColumn2Layout = (RelativeLayout) findViewById(R.id.column2Layout);
        // Hide column2Layout until we have calculated all the area sizes
        mColumn2Layout.setVisibility(INVISIBLE);

        // Trick to get the height of column2Layout
        // By this way, the drawing will fit the space available on this phone
        ViewTreeObserver viewTreeObserver = mColumn2Layout.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver viewTreeObserver = mColumn2Layout.getViewTreeObserver();
                viewTreeObserver.removeOnGlobalLayoutListener(this);

                int column2LayoutHeight = mColumn2Layout.getMeasuredHeight();
                setDrawingSize(column2LayoutHeight);
            }
        });

        mArea1TextView = (TextView) findViewById(R.id.area1TextView);
        mArea1TextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return area1OnTouchEvent(v, event);
            }
        });

        mArea2TextView = (TextView) findViewById(R.id.area2TextView);
        // No onTouchListener for Area2 because it will simply take all the remaining space

        mAndefAreaTextView = (TextView) findViewById(R.id.andefAreaTextView);

        mArea1SizeInBytesTextView = (TextView) findViewById(R.id.area1SizeInBytesTextView);
        mArea1SizeInBlocksTextView = (TextView) findViewById(R.id.area1SizeInBlocksTextView);

        mArea2SizeInBytesTextView = (TextView) findViewById(R.id.area2SizeInBytesTextView);
        mArea2SizeInBlocksTextView = (TextView) findViewById(R.id.area2SizeInBlocksTextView);

        mAndefAreaSizeInBytesTextView = (TextView) findViewById(R.id.andefAreaSizeInBytesTextView);
        mAndefAreaSizeInBlocksTextView = (TextView) findViewById(R.id.andefAreaSizeInBlocksTextView);

        mTotalSizeInBytesTextView = (TextView) findViewById(R.id.totalSizeInBytesTextView);
        mTotalSizeInBlocksTextView = (TextView) findViewById(R.id.totalSizeInBlocksTextView);

        Button upButton = (Button) findViewById(R.id.upButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEndOfArea1(mEndOfArea1 - 1);
                drawAreas();
            }
        });

        Button downButton = (Button) findViewById(R.id.downButton);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEndOfArea1(mEndOfArea1 + 1);
                drawAreas();
            }
        });

        mArea1TextView.setBackgroundResource(R.drawable.shape_area1_selected);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mST25TVCTag);
        if(!tagChanged) {
            readCurrentAreaSizes();
            drawAreas();
        }
    }

    private void setDrawingSize(int column2LayoutHeight) {

        // Areas will be drawn in "column2Layout"
        // Each area will be represented by a rectangle.
        // - Rectangle width will be fixed (arbitrary set to "80dp")
        // - Rectangle height will be relative to column2Layout's height. By this way the drawing
        //   size will fit the screen size.

        // The drawing will occupy 90% of column2Layout's height
        mWholeTagHeightInPixels = (column2LayoutHeight * 90) / 100;

        drawAreas();
    }

    /**
     * This function will read the tag's registers to know the size and position of each area.
     */
    private void readCurrentAreaSizes() {
        new myAsyncTask(Action.READ_REGISTER_VALUES).execute();
    }

    /**
     * This function will write the area sizes to the tag.
     */
    private void writeAreaSizes() {
        new myAsyncTask(Action.WRITE_REGISTER_VALUES).execute();
    }

    private int getArea1SizeInBlocks() {
        int area1SizeInBlocks = mEndOfArea1 + 1;
        return area1SizeInBlocks;
    }

    private int getAndefAreaSizeInBlocks() {
        int andefAreaSizeInBlocks = 0;

        return andefAreaSizeInBlocks;
    }

    private int getMaxEndOfArea1Value() {
        int andefAreaSizeInBlocks = getAndefAreaSizeInBlocks();
        int area1MaxSizeInBlocks = mTotalMemorySizeInBlocks - andefAreaSizeInBlocks;
        int maxEndOfArea1Value = area1MaxSizeInBlocks - 1;
        return maxEndOfArea1Value;
    }

    private void drawAreas() {

        if(mWholeTagHeightInPixels == 0) {
            // Areas cannot be drawn until mWholeTagHeightInPixels is known because all the sizes
            // will be relative to this size
            return;
        }

        if(!mAreAreaSizesKnown) {
            // Areas cannot be drawn until area sizes are known
            return;
        }

        int blockSizeInBytes = mST25TVCTag.getBlockSizeInBytes();

        int area1SizeInBlocks = getArea1SizeInBlocks();
        int andefAreaSizeInBlocks = getAndefAreaSizeInBlocks();
        // Area2 will take all the remaining space
        int area2SizeInBlocks = mTotalMemorySizeInBlocks - area1SizeInBlocks - andefAreaSizeInBlocks;

        int area1RectangleHeight = convertSizeInBlocksIntoRectangleHeight(area1SizeInBlocks);
        int area2RectangleHeight = convertSizeInBlocksIntoRectangleHeight(area2SizeInBlocks);
        int andefAreaRectangleHeight = convertSizeInBlocksIntoRectangleHeight(andefAreaSizeInBlocks);

        mArea1TextView.getLayoutParams().height = area1RectangleHeight;
        mArea1TextView.requestLayout();

        mArea2TextView.getLayoutParams().height = area2RectangleHeight;
        mArea2TextView.requestLayout();

        mAndefAreaTextView.getLayoutParams().height = andefAreaRectangleHeight;
        mAndefAreaTextView.requestLayout();

        // The content of column2Layout is ready
        mColumn2Layout.setVisibility(View.VISIBLE);

        // Update the Table
        mArea1SizeInBytesTextView.setText(String.valueOf(area1SizeInBlocks*blockSizeInBytes));
        mArea1SizeInBlocksTextView.setText(String.valueOf(area1SizeInBlocks));

        mArea2SizeInBytesTextView.setText(String.valueOf(area2SizeInBlocks*blockSizeInBytes));
        mArea2SizeInBlocksTextView.setText(String.valueOf(area2SizeInBlocks));
        mTotalSizeInBytesTextView.setText(String.valueOf(mTotalMemorySizeInBlocks*blockSizeInBytes));
        mTotalSizeInBlocksTextView.setText(String.valueOf(mTotalMemorySizeInBlocks));
    }

    /**
     * This function converts a size in blocks into a rectangle Height.
     * It does a rule of three:
     *      mTotalMemorySizeInBlocks ---> mWholeTagHeightInPixels
     *      sizeInBlocks             ---> rectangleHeight
     * @param sizeInBlocks
     * @return
     */
    private int convertSizeInBlocksIntoRectangleHeight(int sizeInBlocks) {
        int rectangleHeight = (sizeInBlocks * mWholeTagHeightInPixels) / mTotalMemorySizeInBlocks;
        return rectangleHeight;
    }

    /**
     * This function converts a rectangle Height into a size in blocks.
     * It does a rule of three:
     *      mWholeTagHeightInPixels ---> mTotalMemorySizeInBlocks
     *      rectangleHeight         ---> sizeInBlocks
     * @param rectangleHeight
     * @return
     */
    private int convertRectangleHeightIntoSizeInBlocks(int rectangleHeight) {
        int sizeInBlocks = (rectangleHeight * mTotalMemorySizeInBlocks) / mWholeTagHeightInPixels;
        return sizeInBlocks;
    }


    /**
     * Proceed touch events in Area1
     * @param v
     * @param event
     * @return
     */
    public boolean area1OnTouchEvent(View v, MotionEvent event) {
        Log.v(TAG, "Area1 MotionEvent: " + event);

        int height = v.getLayoutParams().height;
        Log.v(TAG, "height="+ height);

        int y = (int)event.getY();
        Log.v(TAG, "y="+ y);

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                // We consume the touch event only if the click was in the last pixels of the area
                if((height - y) < TOUCH_RANGE) {
                    // return true to indicate that we have consummed the ACTION_DOWN event
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int newRectangleHeight = y;

                if(newRectangleHeight < 0) {
                    newRectangleHeight = 0;
                }

                int newArea1Size = convertRectangleHeightIntoSizeInBlocks(newRectangleHeight);
                int endOfArea1 = newArea1Size-1;
                setEndOfArea1(endOfArea1);
                drawAreas();
                break;

            case MotionEvent.ACTION_UP:
                break;
        }

        return false;
    }

    private void setEndOfArea1(int endOfArea1) {

        if(endOfArea1 < 0) {
            endOfArea1 = 0;
        }

        int maxEndOfArea1Value = getMaxEndOfArea1Value();
        if(endOfArea1 > maxEndOfArea1Value) {
            endOfArea1 = maxEndOfArea1Value;
        }

        mEndOfArea1 = endOfArea1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_st25dv_mem_conf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                writeAreaSizes();
                return true;

            case R.id.action_refresh:
                readCurrentAreaSizes();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void displayPasswordDialogBox() {
        Log.v(TAG, "displayPasswordDialogBox");

        final int passworNumber = UIHelper.getConfigurationPasswordNumber(mST25TVCTag);

        // Warning: Function called from background thread! Post a request to the UI thread
        runOnUiThread(new Runnable() {
            public void run() {
                FragmentManager fragmentManager = getSupportFragmentManager();

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        passworNumber,
                        getResources().getString(R.string.enter_configuration_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(fragmentManager, "pwdDialogFragment");
            }
        });

    }

    @Override
    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        if (result == PwdDialogFragment.RESULT_OK) {
            // Config password has been entered successfully so we can now retry to write the register values
            writeAreaSizes();

        } else {
            Log.e(TAG, "Action failed! Tag not updated!");
            showToast(R.string.register_action_not_completed);
        }
    }


    /**
     * Indicates the size in Blocks of the "ANDEF TruST25" area
     * @return
     * @throws STException
     */
    public int getAndefTrust25AreaSizeInBlocks() throws STException {
        int andefTrust25AreaSizeInBlocks = 0;

        return andefTrust25AreaSizeInBlocks;
    }

    /**
     * Function returning the Total memory size (in blocks) including the "ANDEF + TruST25" area.
     * This function is specific to ST25TVC.
     *
     * WARNING: There can be a confusion with getNumberOfBlocks():
     *   getNumberOfBlocks() returns the size of the User Memory Area. "ANDEF + TruST25" area is NOT counted.
     *   getTotalMemorySizeInBlocks() returns the total size, including "ANDEF + TruST25" area.
     *
     * @return
     * @throws STException
     */
    public int getTotalMemorySizeInBlocks() throws STException {
        int totalMemorySizeInBlocks = mST25TVCTag.getNumberOfBlocks() + getAndefTrust25AreaSizeInBlocks();
        return totalMemorySizeInBlocks;
    }


    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                if(mAction == Action.READ_REGISTER_VALUES) {

                    mEndOfArea1 = mST25TVCTag.getClampedArea1EndValue();
                    mTotalMemorySizeInBlocks = getTotalMemorySizeInBlocks();
                    mAreAreaSizesKnown = true;
                    result = ACTION_SUCCESSFUL;

                } else {
                    // Write Register values

                    if (mIsArea1ConfigurationLocked) {
                        return AREA1_CONFIGURATION_IS_LOCKED;
                    }

                    mST25TVCTag.getRegisterEndArea1().setRegisterValue(mEndOfArea1);
                    result = ACTION_SUCCESSFUL;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case CONFIG_PASSWORD_NEEDED:
                        result = CONFIG_PASSWORD_NEEDED;
                        break;

                    case TAG_NOT_IN_THE_FIELD:
                        result = TAG_NOT_IN_THE_FIELD;
                        break;

                    default:
                        e.printStackTrace();
                        result = ACTION_FAILED;
                        break;
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    if(mAction == Action.READ_REGISTER_VALUES) {
                        // Now that area sizes are known, we can refresh the drawing
                        drawAreas();
                    } else {
                        // Write successful
                        DisplayTapTagRequest.run(ST25TVCChangeMemConf.this, mST25TVCTag, getString(R.string.please_tap_the_tag_again));
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayPasswordDialogBox();
                    break;

                case AREA1_CONFIGURATION_IS_LOCKED:
                    UIHelper.displayMessage(ST25TVCChangeMemConf.this, R.string.area1_configuration_is_locked);
                    break;

                case ACTION_FAILED:
                    if(mAction == Action.READ_REGISTER_VALUES) {
                        showToast(R.string.error_while_reading_the_tag);
                        mColumn2Layout.setVisibility(INVISIBLE);
                    } else {
                        showToast(R.string.error_while_updating_the_tag);
                    }
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    mColumn2Layout.setVisibility(INVISIBLE);
                    break;
            }

            return;
        }
    }


}
