package com.mehtank.androminion.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.mehtank.androminion.ui.Strings;
import com.vdom.core.VDomServer;

public class DominionServer extends Service {

  @SuppressWarnings("unused")
  private static final String TAG = "DominionServer";
  private static final String stopped = "Server stopped";
  VDomServer vds;

  @Override
  public void onCreate() {
    super.onCreate();
    if (!test().equals(stopped)) {
      return;
    }

    Strings.initContext(this);
    VDomServer.main(new String[] {
      "Drew (AI)", "com.vdom.players.VDomPlayerDrew",
      "Earl (AI)", "com.vdom.players.VDomPlayerEarl",
      "Mary (AI)", "com.vdom.players.VDomPlayerMary",
      "Chuck (AI)", "com.vdom.players.VDomPlayerChuck",
      "Sarah (AI)", "com.vdom.players.VDomPlayerSarah",
      "Patrick (AI)", "com.vdom.players.VDomPlayerPatrick",
      //                "-debug"
    });
    vds = VDomServer.me;
  }

  @Override
  public void onDestroy() {
    if (vds != null) {
      try {
        vds.quit();
      } catch (NullPointerException e) {
        // whatever.
      }
    }
    vds = null;
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public String getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
          enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            return inetAddress.getHostAddress();
          }
        }
      }
    } catch (SocketException ex) {
      Log.e("DroidServer", ex.toString());
    }
    return null;
  }

  String test() {
    if (vds == null) {
      return stopped;
    }

    int port = vds.getPort();
    if (port == 0) {
      return stopped;
    }

    String host = getLocalIpAddress();
    return "Server started on " + (host == null ? "localhost" : host) + ":" + port;
  }

}
