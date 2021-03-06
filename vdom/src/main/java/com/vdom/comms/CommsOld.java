package com.vdom.comms;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class CommsOld implements Runnable {

  static final int TIMEOUT = 15000; // 15 seconds in ms
  static final boolean DEBUGGING = false;

  String host;
  int port;
  EventHandler parent;
  MonitorObject myMonitorObject = new MonitorObject();
  Event latestEvent = null;
  private boolean isServer = true;
  private ServerSocket pserver = null;
  private Socket pclient = null;
  private ObjectInputStream ois = null;
  private ObjectOutputStream oos = null;

  private boolean done = false;

  public CommsOld(EventHandler parent, int port) throws IOException {
    this.parent = parent;
    isServer = true;
    this.port = port;

    debug("Opening server socket...");
    pserver = new ServerSocket(this.port);
    host = pserver.getInetAddress().getHostAddress();
    debug("Opened: " + host + " / " + port);
  }

  public CommsOld(EventHandler parent, String host, int port) {
    this.parent = parent;
    isServer = false;
    this.host = host;
    this.port = port;
  }

  public Event doWait() {
    Event e;
    synchronized (myMonitorObject) {
      while (latestEvent == null) {
        try {
          myMonitorObject.wait(TIMEOUT);
        } catch (InterruptedException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        if (done) {
          return null;
        }
      }
      //clear signal and continue running.
      e = latestEvent;
      latestEvent = null;
    }
    return e;
  }

  public void doNotify(Event e) {
    synchronized (myMonitorObject) {
      latestEvent = e;
      myMonitorObject.notify();
    }
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isConnected() {
    return (pclient == null ? false : pclient.isConnected());
  }

  public void connect() throws UnknownHostException, IOException, StreamCorruptedException {
    if (isConnected()) {
      return;
    }

    // open a socket connection
    if (isServer) {
      pclient = pserver.accept();
    } else {
      pclient = new Socket(host, port);
    }

    // Set read timeout, double for servers than for clients
    pclient.setSoTimeout(TIMEOUT * (isServer ? 2 : 1));

    // open I/O streams for objects
    oos = new ObjectOutputStream(pclient.getOutputStream());
    ois = new ObjectInputStream(pclient.getInputStream());
  }

  public Event get() throws IOException {
    Event p = null;
    try {
      // debug("Trying to get...");
      p = (Event) ois.readObject();
      debug("Got: " + p);
    } catch (OptionalDataException e) {
      debug("OptionalDataException in Comms.get() -- ignoring.");
    } catch (ClassNotFoundException e) {
      debug("ClassNotFoundException in Comms.get() -- ignoring.");
    } catch (NullPointerException e) {
      debug("NullPointerException in Comms.get() -- ignoring.");
    }

    return p;
  }

  public void put(Event p) throws IOException {
    debug("Put: " + p);
    oos.writeObject(p);
  }

  @Override
  public void run() {
    done = false;
    if (!isConnected()) {
      if (isServer) {
        try {
          connect();
        } catch (Exception e) {
          debug("Failed to connect in run!");
          return;
        }
      } else {
        return;
      }
    }

    boolean disconnect = false;

    while (!done) {
      Event p = null;
      boolean timeout = false;
      try {
        p = get();
      } catch (SocketTimeoutException e) {
        debug("Connection timed out...");
        timeout = true;
      } catch (EOFException e) {
        debug("Socket externally closed in Comms.run() -- quitting.");
        disconnect = true;
      } catch (Exception e) {
        debug("Other exception in Comms.run() -- quitting.");
        e.printStackTrace();
        disconnect = true;
      }

      if (done) {
        break;
      }

      if ((p != null) && (p.t == Event.EType.PING)) {
        try {
          put(new Event(Event.EType.PONG));
        } catch (Exception e) {
          debug("Could not pong in Comms.run() -- quitting.");
          e.printStackTrace();
          disconnect = true;
        }
      } else if ((p != null) && (p.t == Event.EType.GETSERVER)) {
        try {
          put(new Event(Event.EType.SERVER).setString(host).setInteger(port));
        } catch (Exception e) {
          debug("Could not pong server in Comms.run() -- quitting.");
          e.printStackTrace();
          disconnect = true;
        }
      } else if (p != null) {
        if (!parent.handle(p)) {
          doNotify(p);
        }
      } else if (timeout) {
        disconnect = ping();
      }

      if (disconnect) {
        p = new Event(Event.EType.DISCONNECT);
        if (!parent.handle(p)) {
          doNotify(p);
        }
        done = true;
      }
    }

    disconnect();
    debug("End of Comms.run()");
  }

  public boolean stop() {
    done = true;
    return disconnect();
  }

  private void debug(String s) {
    s = host + ":" + port + " -- " + s;
    // System.err.println (":: Androminion :: " + s);
    if (DEBUGGING) {
      parent.debug(s);
    }
  }

  private boolean disconnect() {
    debug("Shutting down...");

    boolean clean = true;
    try {
      // close I/O streams
      pclient.shutdownInput();
      pclient.shutdownOutput();
      debug("Streams shutdown.");
    } catch (Exception e) {
      clean = false;
    }

    try {
      oos.close();
      ois.close();
      debug("Streams closed.");
    } catch (Exception e) {
      clean = false;
    }

    try {
      // close socket connection
      pclient.close();
      debug("Socket closed.");
    } catch (Exception e) {
      clean = false;
    }

    if (isServer) {
      // close server
      // debug ("Stopping server...");
      try {
        pserver.close();
        debug("Server stopped");
      } catch (Exception e) {
        clean = false;
      }
    }

    ois = null;
    oos = null;
    pclient = null;
    pserver = null;
    return clean;
  }

  private boolean ping() {
    try {
      put(new Event(Event.EType.PING));
    } catch (Exception e) {
      debug("Exception in Comms.ping() while sending -- quitting.");
      e.printStackTrace();
      return true;
    }

    Event p;

    try {
      p = get();
    } catch (SocketTimeoutException e) {
      debug("Timed out in Comms.ping() -- quitting.");
      return true;
    } catch (Exception e) {
      debug("Exception in Comms.ping() while recving -- quitting.");
      e.printStackTrace();
      return true;
    }

    if ((p == null) || (p.t != Event.EType.PONG)) {
      debug("Invalid packet in Comms.ping() -- quitting.");
      return true;
    }

    return false;
  }

  public static class MonitorObject {

  }

}
