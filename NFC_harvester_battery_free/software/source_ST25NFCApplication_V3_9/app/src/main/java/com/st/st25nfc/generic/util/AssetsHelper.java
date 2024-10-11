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
package com.st.st25nfc.generic.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.st.st25nfc.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by MMY on 9/11/2018.
 */

public class AssetsHelper {
    static final String TAG = "AssetsHelper";

    private static String LICENSE_FILE_NAME = "ST25SWLicense.txt";

    public static String getLicenseFileName() {
        return LICENSE_FILE_NAME;
    }


    public static boolean isFileExistingInAssetsDir(String pathInAssetsDir, AssetManager assetManager){
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(pathInAssetsDir);
            if(null != inputStream ) {
                return true;
            }
        }  catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static byte[] getSTLicense(String pathInAssetsDir, AssetManager assetManager) {
        byte[] buffer = null;
        try {
            InputStream is = assetManager.open(pathInAssetsDir);

            // We guarantee that the available method returns the total
            // size of the asset...  of course, this does mean that a single
            // asset can't be more than 2 gigs.
            int size = is.available();

            // Read the entire asset into a local byte buffer.
            buffer = new byte[size];
            is.read(buffer);
            is.close();

        } catch (IOException e) {
            // Should never happen!
            throw new RuntimeException(e);
        }
        return buffer;
    }

    public static void displayLicense(AssetManager assetManager, Context ctx) {
        if (AssetsHelper.isFileExistingInAssetsDir(AssetsHelper.getLicenseFileName(),assetManager)) {
            byte[] buffer = AssetsHelper.getSTLicense(AssetsHelper.getLicenseFileName(),assetManager);
            String message = new String(buffer);
            AlertDialog licenceDialog;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

            // set title
            alertDialogBuilder.setTitle(ctx.getString(R.string.for_your_information));

            // set dialog message
            alertDialogBuilder
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(ctx.getString(R.string.ok),new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            dialog.cancel();

                        }
                    });

            // create alert dialog
            licenceDialog = alertDialogBuilder.create();
            // show it
            licenceDialog.show();
        } else {
            // Display toast - no License
            Toast.makeText(ctx, R.string.no_license_information, Toast.LENGTH_LONG).show();

        }

    }
}
