package ru.org.amip.MarketAccess.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import ru.org.amip.MarketAccess.R;

/**
 * Date: Mar 24, 2010
 * Time: 4:14:07 PM
 *
 * @author serge
 */
public class RunWithProgress implements Runnable {
  private static String[] writePropCommand;
  private ProgressDialog pd;
  private final Context ctx;
  private final String message;
  private CompleteListener completeListener;
  private boolean silent;

  private static final String[] COMMANDS = new String[]{
    "setprop gsm.sim.operator.numeric",
    "killall com.android.vending",
    "rm -rf /data/data/com.android.vending/cache/*",
    "chmod 777 /data/data/com.android.vending/shared_prefs",
    "chmod 666 /data/data/com.android.vending/shared_prefs/vending_preferences.xml",
    "setpref com.android.vending vending_preferences boolean metadata_paid_apps_enabled true",
    "chmod 660 /data/data/com.android.vending/shared_prefs/vending_preferences.xml",
    "chmod 771 /data/data/com.android.vending/shared_prefs",
    "setown com.android.vending /data/data/com.android.vending/shared_prefs/vending_preferences.xml"
  };

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if (!silent) showProgress(msg);
      if (completeListener != null) {
        completeListener.onComplete();
      }
    }
  };

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public void setCompleteListener(CompleteListener completeListener) {
    this.completeListener = completeListener;
  }

  public void showProgress(Message msg) {
    if (msg.arg2 != 0) {
      pd.setProgress(msg.arg1);
    } else {
      pd.dismiss();
      if (msg.arg1 == 0) {
        Toast.makeText(ctx, R.string.applied, Toast.LENGTH_SHORT).show();
      } else if (msg.arg1 == 1) {
        Toast.makeText(ctx, R.string.error, Toast.LENGTH_LONG).show();
      } else {
        showNoRootAlert();
      }
    }
  }

  private void showNoRootAlert() {
    new AlertDialog.Builder(ctx)
      .setMessage(R.string.no_root)
      .setCancelable(false)
      .setPositiveButton(R.string.no_root_ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      }).create().show();
  }

  public static String[] makeCommand(String numeric) {
    final String[] strings = new String[COMMANDS.length];
    System.arraycopy(COMMANDS, 0, strings, 0, COMMANDS.length);
    strings[0] = strings[0] + ' ' + numeric;
    return strings;
  }

  public RunWithProgress(Context ctx, String value, String message) {
    this.ctx = ctx;
    this.message = message;

    writePropCommand = makeCommand(value);
  }

  public void doRun() {
    if (!silent) {
      pd = new ProgressDialog(ctx);
      pd.setMax(writePropCommand.length);
      pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      pd.setProgress(1);
      pd.setTitle(R.string.working);
      pd.setMessage(message);
      pd.show();
    }
    new Thread(this).start();
  }

  @Override
  public void run() {
    if (!ShellInterface.isSuAvailable()) {
      Message msg = Message.obtain();
      msg.arg1 = 2;
      msg.arg2 = 0;
      handler.sendMessage(msg);
      return;
    }
    ShellInterface.doExec(writePropCommand, true, handler);
  }
}