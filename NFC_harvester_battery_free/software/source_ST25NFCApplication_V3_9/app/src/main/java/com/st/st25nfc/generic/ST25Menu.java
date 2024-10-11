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

package com.st.st25nfc.generic;

import android.app.Activity;
import com.google.android.material.navigation.NavigationView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.st.st25nfc.R;
import com.st.st25nfc.type5.GenericType5Menu;
import com.st.st25nfc.type5.st25dv.ST25DVMenu;
import com.st.st25nfc.type5.st25dvpwm.ST25DVWMenu;
import com.st.st25nfc.type5.st25tv.ST25TVMenu;
import com.st.st25nfc.type5.st25tvc.ST25TVCMenu;
import com.st.st25nfc.type5.stlri.STLRiMenu;
import com.st.st25nfc.type5.stlri.STLRiS2kMenu;
import com.st.st25nfc.type5.stlri.STLRiS64kMenu;
import com.st.st25nfc.type5.stm24lr.STLRMenu;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.type2.Type2Tag;
import com.st.st25sdk.type2.st25tn.ST25TNTag;
import com.st.st25sdk.type4a.Type4Tag;
import com.st.st25sdk.type5.Type5Tag;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import java.util.ArrayList;
import java.util.List;


public class ST25Menu {

    private static ST25Menu mMenu;
    protected List<Integer> mMenuResource;
    private static final String DBG_LOG = "ST25Menu";
    private static String mTagName;

    public static ST25Menu newInstance(NFCTag tag) {
        String tagName;
        if (tag == null) {
            mMenu = null;
            Log.e(DBG_LOG, "Menu not defined for this tag! - tag is null");
            return mMenu;
        }

        tagName = tag.getName();
        setProductName(tagName);
        if ( (tagName.contains("LRi2K")) || (tagName.contains("LRi1K")) || (tagName.contains("LRi512")) ) {
            mMenu = new STLRiMenu(tag);
        } else if (tagName.contains("LRiS2K")) {
            mMenu = new STLRiS2kMenu(tag);
        } else if (tagName.contains("LRiS64K")) {
            mMenu = new STLRiS64kMenu(tag);
        }else if (tagName.contains("ST25DV02K-W")) {
            mMenu = new ST25DVWMenu(tag);
        } else if ((tagName.contains("ST25DV")) || (tagName.contains("ST25TV64K")) || (tagName.contains("ST25TV16K")) || (tagName.contains("ST25TV04K-P"))) {
            mMenu = new ST25DVMenu(tag);
        } else if ((tagName.contains("ST25TV512")) || (tagName.contains("ST25TV02K")) || (tagName.contains("ST25TV01K"))) {
            if (tag instanceof ST25TVCTag) {
                mMenu = new ST25TVCMenu(tag);
            } else {
                mMenu = new ST25TVMenu(tag);
            }
        } else if (tagName.contains("M24LR")) {
            mMenu = new STLRMenu(tag);
        } else if (tag instanceof Type5Tag) {
            mMenu = new GenericType5Menu(tag);
        }

        else {
            mMenu = null;
            Log.e(DBG_LOG, "Menu not defined for this tag!");
        }
        return mMenu;
    }

    private static void setProductName(String name) {
        mTagName = name;
    }
    private  String getProductName() {
        return mTagName;
    }
    public static ST25Menu getInstance(NFCTag tag) {
        if (mMenu == null) {
            mMenu = new ST25Menu(tag);
        }
        return mMenu;
    }

    public ST25Menu(NFCTag tag) {
        mMenuResource = new ArrayList<Integer>();

        // By default every menu will have the entries of menu_home.xml
        mMenuResource.add(R.menu.menu_home);
    }

    public boolean selectItem(Activity activity, MenuItem item) {
        return false;
    }

    public void inflateMenu(NavigationView view) {
        if (mMenuResource != null) {
            for (Integer menu : mMenuResource) {
                view.inflateMenu(menu);
            }
        }

        // Add help menu
        view.inflateMenu(R.menu.menu_help);

        // update the Home name
        // get menu from navigationView
        Menu menu = view.getMenu();
        // find MenuItem you want to change
        MenuItem nav_item = menu.findItem(R.id.product_name);
        if (nav_item != null) nav_item.setTitle(getProductName());

    }
}
