package cm.aptoide.pt.spotandshareandroid;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.entities.FileInfo;
import cm.aptoide.pt.spotandshare.socket.exception.ServerLeftException;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileClientLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileServerLifecycle;
import cm.aptoide.pt.spotandshare.socket.message.client.AptoideMessageClientSocket;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.StorageCapacity;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.RequestPermissionToSend;
import cm.aptoide.pt.utils.AptoideUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by filipegoncalves on 10-02-2017.
 */

public class HighwayClientService extends Service {

  public static final int INSTALL_APP_NOTIFICATION_REQUEST_CODE = 147;
  private final int PROGRESS_SPLIT_SIZE = 10;
  private int port;
  private ArrayList<App> listOfApps;
  private NotificationManagerCompat mNotifyManager;
  private FileServerLifecycle<AndroidAppInfo> fileServerLifecycle;
  private FileClientLifecycle<AndroidAppInfo> fileClientLifecycle;
  private AptoideMessageClientSocket aptoideMessageClientSocket;

  @Override public void onCreate() {
    super.onCreate();
    System.out.println("Inside the onCreate of the service");

    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }

    fileClientLifecycle = new FileClientLifecycle<AndroidAppInfo>() {

      private ProgressFilter progressFilter;

      @Override public void onError(IOException e) {
        System.out.println("Fell on error  Client !! ");

        Intent i = new Intent();
        if (e instanceof ServerLeftException) {
          i.setAction("SERVER_LEFT");
        } else {
          i.setAction("ERRORRECEIVING");
        }
        sendBroadcast(i);
      }

      @Override public void onStartReceiving(AndroidAppInfo androidAppInfo) {
        System.out.println(" Started receiving ");

        progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

        //show notification
        createReceiveNotification(androidAppInfo.getAppName());

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override public void onFinishReceiving(AndroidAppInfo androidAppInfo) {
        System.out.println(" Finished receiving " + androidAppInfo);

        finishReceiveNotification(androidAppInfo.getApk().getFilePath(),
            androidAppInfo.getPackageName(), androidAppInfo);

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("filePath", androidAppInfo.getApk().getFilePath());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {
        if (progressFilter.shouldUpdate(progress)) {
          int actualProgress = Math.round(progress * PROGRESS_SPLIT_SIZE);
          showReceiveProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
        }
      }
    };

    fileServerLifecycle = new FileServerLifecycle<AndroidAppInfo>() {

      private ProgressFilter progressFilter;

      @Override public void onStartSending(AndroidAppInfo o) {
        System.out.println(" Started sending ");

        progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

        Intent i = new Intent();
        i.putExtra("isSent", false);
        i.putExtra("needReSend",
            false);//add field with pos to resend and change its value only if it is != 100000 (onstartcommand)
        i.putExtra("appName", o.getAppName());
        i.putExtra("packageName", o.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);

        createSendNotification();
      }

      @Override public void onFinishSending(AndroidAppInfo o) {
        System.out.println(" Finished sending " + o);

        finishSendNotification(o);

        Intent i = new Intent();
        i.putExtra("isSent", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", o.getAppName());
        i.putExtra("packageName", o.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);
      }

      @Override public void onError(IOException e) {
        e.printStackTrace();

        Intent i = new Intent();
        i.setAction("ERRORSENDING");
        sendBroadcast(i);
      }

      @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {

        if (progressFilter.shouldUpdate(progress)) {
          int actualProgress = Math.round(progress * 100);
          showSendProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
        }
      }
    };
  }

  private void createReceiveNotification(String receivingAppName) {
    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.receive))
        .setContentText(this.getResources().getString(R.string.receiving) + " " + receivingAppName)
        .setSmallIcon(R.mipmap.ic_launcher);
  }

  private void finishReceiveNotification(String receivedApkFilePath, String packageName,
      AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.receive))
        .setContentText(this.getResources().getString(R.string.transfCompleted))
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setProgress(0, 0, false)
        .setAutoCancel(true);

    Intent intent = new Intent();
    intent.setAction("INSTALL_APP_NOTIFICATION");
    intent.putExtra("filePath", receivedApkFilePath);
    intent.putExtra("packageName", packageName);
    PendingIntent contentIntent =
        PendingIntent.getBroadcast(this, INSTALL_APP_NOTIFICATION_REQUEST_CODE, intent,
            PendingIntent.FLAG_CANCEL_CURRENT);

    mBuilderReceive.setContentIntent(contentIntent);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(), mBuilderReceive.build());
  }

  private void showReceiveProgress(String receivingAppName, int actual,
      AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.receive))
        .setContentText(this.getResources().getString(R.string.receiving) + " " + receivingAppName)
        .setSmallIcon(R.mipmap.ic_launcher);

    mBuilderReceive.setProgress(100, actual, false);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(), mBuilderReceive.build());
  }

  private void createSendNotification() {

    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.send))
        .setContentText(this.getResources().getString(R.string.preparingSend))
        .setSmallIcon(R.mipmap.ic_launcher);
  }

  private void finishSendNotification(AndroidAppInfo androidAppInfo) {
    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.send))
        .setContentText(this.getResources().getString(R.string.transfCompleted))
        // Removes the progress bar
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setProgress(0, 0, false)
        .setAutoCancel(true);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(), mBuilderSend.build());
  }

  private void showSendProgress(String sendingAppName, int actual, AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(
        this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
            .getString(R.string.send))
        .setContentText(this.getResources().getString(R.string.sending) + " " + sendingAppName)
        .setSmallIcon(R.mipmap.ic_launcher);

    mBuilderSend.setProgress(100, actual, false);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(), mBuilderSend.build());
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {

    if (intent != null) {

      if (intent.getAction() != null && intent.getAction().equals("RECEIVE")) {
        String serverIP = intent.getStringExtra("targetIP");
        port = intent.getIntExtra("port", 0);

        final String externalStoragepath = intent.getStringExtra("ExternalStoragePath");

        StorageCapacity storageCapacity = new StorageCapacity() {
          @Override public boolean hasCapacity(long bytes) {
            long availableSpace = -1L;
            StatFs stat = new StatFs(externalStoragepath);
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
            return availableSpace > bytes;
          }
        };

        aptoideMessageClientSocket =
            new AptoideMessageClientSocket(serverIP, "192.168.43.1", port, externalStoragepath,
                storageCapacity, fileServerLifecycle, fileClientLifecycle);
        aptoideMessageClientSocket.startAsync();

        System.out.println(" Connected ! ");
      } else if (intent.getAction() != null && intent.getAction().equals("SEND")) {
        Bundle b = intent.getBundleExtra("bundle");

        listOfApps = b.getParcelableArrayList("listOfAppsToInstall");
        for (int i = 0; i < listOfApps.size(); i++) {

          String filePath = listOfApps.get(i).getFilePath();
          String appName = listOfApps.get(i).getAppName();
          String packageName = listOfApps.get(i).getPackageName();
          String obbsFilePath = listOfApps.get(i).getObbsFilePath();

          List<FileInfo> fileInfoList = getFileInfo(filePath, obbsFilePath);

          final AndroidAppInfo appInfo = new AndroidAppInfo(appName, packageName, fileInfoList);

          AptoideUtils.ThreadU.runOnIoThread(new Runnable() {
            @Override public void run() {
              aptoideMessageClientSocket.send(
                  new RequestPermissionToSend(aptoideMessageClientSocket.getLocalhost(), appInfo));
            }
          });
        }
      } else if (intent.getAction() != null && intent.getAction().equals("DISCONNECT")) {
        System.out.println("Requested to disconnect !");
        AptoideUtils.ThreadU.runOnIoThread(new Runnable() {
          @Override public void run() {
            if (aptoideMessageClientSocket != null) {
              aptoideMessageClientSocket.exit();
            }
            Intent i = new Intent();
            i.setAction("CLIENT_DISCONNECT");
            sendBroadcast(i);
          }
        });
      }
    }

    return START_STICKY;
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  public List<FileInfo> getFileInfo(String filePath, String obbsFilePath) {
    List<FileInfo> fileInfoList = new ArrayList<>();
    //getApk
    File apk = new File(filePath);
    FileInfo apkFileInfo = new FileInfo(apk);
    fileInfoList.add(apkFileInfo);
    //getObbs

    if (!obbsFilePath.equals("noObbs")) {
      File obbFolder = new File(obbsFilePath);
      File[] list = obbFolder.listFiles();
      if (list != null) {
        if (list.length > 0) {
          for (int i = 0; i < list.length; i++) {
            fileInfoList.add(new FileInfo(list[i]));
          }
        }
      }
    }

    return fileInfoList;
  }
}
