package a860014.mpf.ble_display.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

public class util_permission {

    /**
     * 開啟app設定
     */
    public static void openAppSetting(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);

        // 上面的設定就可以開啟App設定，
        // 以下是為了避免殘留頁面或出現在最近使用的列表而加
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        context.startActivity(intent);
    }

    public static void showDialog(Context context, int messageResId, int btnTextResId, DialogInterface.OnClickListener onBtnClickListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage(messageResId)
                .setPositiveButton(btnTextResId, onBtnClickListener).create();
        dialog.show();
    }
}
