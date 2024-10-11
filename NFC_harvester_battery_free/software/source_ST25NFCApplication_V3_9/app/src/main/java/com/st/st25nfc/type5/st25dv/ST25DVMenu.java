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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.MenuItem;

import com.st.st25nfc.generic.AppLauncherActivity;
import com.st.st25nfc.generic.DynamicRegisterActivity;
import com.st.st25nfc.generic.PreferredApplicationActivity;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.type5.Type5ConfigurationProtectionActivity;
import com.st.st25nfc.type5.Type5SendCustomCmdActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.AreasEditorActivity;
import com.st.st25nfc.generic.RegistersActivity;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25sdk.type5.st25dv.ST25TV04KPTag;
import com.st.st25sdk.type5.st25dv.ST25TV16KTag;
import com.st.st25sdk.type5.st25dv.ST25TV64KTag;

public class ST25DVMenu  extends ST25Menu {
    public ST25DVMenu(NFCTag tag) {
        super(tag);
        mMenuResource.add(R.menu.menu_nfc_forum);

        if ((tag instanceof ST25TV04KPTag) || (tag instanceof ST25TV16KTag) || (tag instanceof ST25TV64KTag)) {
            mMenuResource.add(R.menu.menu_st25tv64k);
        } else {
            mMenuResource.add(R.menu.menu_st25dv);
        }

    }

    @Override
    public boolean selectItem(Activity activity, MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;
        int itemId = item.getItemId();

        // Remove unwanted screens and add Densor specific screens/fragments.
        switch (itemId) {
            case R.id.preferred_application:
                intent = new Intent(activity, PreferredApplicationActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.about:
                UIHelper.displayAboutDialogBox(activity);
                break;
            case R.id.product_name:
            // Nfc forum
            case R.id.tag_info:
                intent = new Intent(activity, ST25DVActivity.class);
                intent.putExtra("select_tab", 0);
                activity.startActivityForResult(intent, 1);
                break;
            // Densor features
            case R.id.time_sync:
                intent = new Intent(activity, ST25DVActivity.class);
                intent.putExtra("select_tab", 1);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.densor_settings:
                intent = new Intent(activity, ST25DVActivity.class);
                intent.putExtra("select_tab", 2);
                activity.startActivityForResult(intent, 1);
                break;
            // Product features
            case R.id.memory_dump:
                intent = new Intent(activity, ST25DVActivity.class);
                intent.putExtra("select_tab", 3);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.sys_file:
                intent = new Intent(activity, ST25DVActivity.class);
                intent.putExtra("select_tab", 4);
                activity.startActivityForResult(intent, 1);
                break;
//            case R.id.app_launcher:
//                intent = new Intent(activity, AppLauncherActivity.class);
//                activity.startActivityForResult(intent, 1);
//                break;
//            case R.id.send_custom_cmd:
//                intent = new Intent(activity, Type5SendCustomCmdActivity.class);
//                activity.startActivityForResult(intent, 1);
//                break;
            case R.id.configuration:
                intent = new Intent(activity, ST25DVChangeMemConf.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.register_management:
                intent = new Intent(activity, RegistersActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
            case R.id.dynamic_register_management:
                intent = new Intent(activity, DynamicRegisterActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
//            case R.id.configuration_protection:
//                intent = new Intent(activity, Type5ConfigurationProtectionActivity.class);
//                activity.startActivityForResult(intent, 1);
//                break;
//            case R.id.areas_ndef_editor:
//                intent = new Intent(activity, AreasEditorActivity.class);
//                activity.startActivityForResult(intent, 1);
//                break;
//            case R.id.area_security_status_management:
//                intent = new Intent(activity, ST25DVAreaSecurityStatusActivity.class);
//                activity.startActivityForResult(intent, 1);
//                break;
            case R.id.mailbox_management:
                intent = new Intent(activity, ST25DVMailboxActivity.class);
                activity.startActivityForResult(intent, 1);
                break;
        }
        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
