/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package tls.rub.evolutionaryfuzzer;

import java.util.ArrayList;


public class ServerManager
{

    /**
     * Singleton
     *
     * @return Instance of the ServerManager
     */
    public static ServerManager getInstance() {
        return ServerManagerHolder.INSTANCE;
    }

    private ArrayList<TLSServer> serverList;

    private ServerManager()
    {
        serverList = new ArrayList<>();
    }

    /**
     * Adds a TLSServer to the List of TLSServers
     *
     * @param server
     */
    public void addServer(TLSServer server)
    {
        serverList.add(server);
    }

    /**
     * Trys to get an unused Server from the ServerList. Starts over if there is
     * no free Server available. If it still searches for a free Server after 10
     * seconds, it throws an Exception. If a server is found, the Server is
     * reserved. Its the caller duty to release the Server once it is finished.
     *
     * @return A Free Server
     */
    public synchronized TLSServer getFreeServer()
    {
        //System.out.println("Getting Server");
        long startSearch = System.currentTimeMillis();
        if (serverList.isEmpty())
        {
            return null;
        }
        int i = 0;
        while (true)
        {
            TLSServer server = serverList.get(i % serverList.size());
            if (server.isFree())
            {
                //Try to get a free Server

                server.occupie();
                //System.out.println("Got:"+server.toString());
                return server;
            }
            i++;
            if (startSearch < System.currentTimeMillis() - 60000)
            {
                //Searched longer than a minute and didnt find a free Server
                throw new RuntimeException("Could not find a free Server, if you have >= #servers than #executors there is a bug in the Code that causes Servers to not be properly released or not restart properly.");
            }
        }
    }

    /**
     * Removes all Server from the ServerList. This method is mostly Implemented
     * for UnitTesting purposes.
     */
    public void clear()
    {
        serverList = new ArrayList<>();
    }


    //Singleton

    private static class ServerManagerHolder
    {

        private static final ServerManager INSTANCE = new ServerManager();
    }
}
