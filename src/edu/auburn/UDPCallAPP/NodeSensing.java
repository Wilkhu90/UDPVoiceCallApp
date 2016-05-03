package edu.auburn.UDPCallAPP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
* Performs broadcast node detection.
* 
* @author Sumeet
*/
public class NodeSensing {
private static final byte QUERY_PACKET = 80;

private static final byte RESPONSE_PACKET = 81;

/**
 * The group identifier. Determines the set of nodes that are able
 * to discover each other
 */
public final int group;

/**
 * The port number that we operate on
 */
public final int port;

/**
 * Data returned with discovery
 */
public int nodeData;

private final DatagramSocket bcastSocket;

private final InetSocketAddress broadcastAddress;

private boolean shouldStop = false;

private List<Node> responseList = null;

/**
 * Used to detect and ignore this nodes response to it's own query.
 * When we send a response packet, we set this to the destination.
 * When we receive a response, if this matches the source, we know
 * that we're talking to ourselves and we can ignore the response.
 */
private InetAddress lastResponseDestination = null;

/**
 * Redefine this to be notified of exceptions on the listen thread.
 * Default behaviour is to print to stdout. Can be left as null for
 * no-op
 */
public ExceptionHandler rxExceptionHandler = new ExceptionHandler();

private Thread bcastListen = new Thread( NodeSensing.class.getSimpleName()
    + " broadcast listen thread" ) {
  @Override
  public void run()
  {
    try
    {
      byte[] buffy = new byte[ 5 ];
      DatagramPacket rx = new DatagramPacket( buffy, buffy.length );

      while( !shouldStop )
      {
        try
        {
          buffy[ 0 ] = 0;

          bcastSocket.receive( rx );

          int recData = decode( buffy, 1 );

          if( buffy[ 0 ] == QUERY_PACKET && recData == group )
          {
            byte[] data = new byte[ 5 ];
            data[ 0 ] = RESPONSE_PACKET;
            encode( nodeData, data, 1 );

            DatagramPacket tx =
                new DatagramPacket( data, data.length, rx.getAddress(), port );

            lastResponseDestination = rx.getAddress();

            bcastSocket.send( tx );
          }
          else if( buffy[ 0 ] == RESPONSE_PACKET )
          {
            if( responseList != null && !rx.getAddress().equals( lastResponseDestination ) )
            {
              synchronized( responseList )
              {
                responseList.add( new Node( rx.getAddress(), recData ) );
              }
            }
          }
        }
        catch( SocketException se )
        {
          // someone may have called disconnect()
        }
      }

      bcastSocket.disconnect();
      bcastSocket.close();
    }
    catch( Exception e )
    {
      if( rxExceptionHandler != null )
      {
        rxExceptionHandler.handle( e );
      }
    }
  };
};

/**
 * Constructs a UDP broadcast-based node
 * 
 * @param group
 *           The identifier shared by the nodes that will be
 *           discovered.
 * @param port
 *           a valid port, i.e.: in the range 1025 to 65535
 *           inclusive
 * @throws IOException
 */
public NodeSensing( int group, int port ) throws IOException
{
  this.group = group;
  this.port = port;

  bcastSocket = new DatagramSocket( port );
  broadcastAddress = new InetSocketAddress( "255.255.255.255", port );

  bcastListen.setDaemon( true );
  bcastListen.start();
}

/**
 * Signals this {@link nodeDiscovery} to shut down. This call will
 * block until everything's timed out and closed etc.
 */
public void disconnect()
{
  shouldStop = true;

  bcastSocket.close();
  bcastSocket.disconnect();

  try
  {
    bcastListen.join();
  }
  catch( InterruptedException e )
  {
    e.printStackTrace();
  }
}

/**
 * Queries the network and finds the addresses of other nodes in
 * the same group
 * 
 * @param timeout
 *           How long to wait for responses, in milliseconds. Call
 *           will block for this long, although you can
 *           {@link Thread#interrupt()} to cut the wait short
 * @param nodeType
 *           The type flag of the nodes to look for
 * @return The addresses of other nodes in the group
 * @throws IOException
 *            If something goes wrong when sending the query packet
 */
public Node[] getNodes( int timeout, byte nodeType ) throws IOException
{
  responseList = new ArrayList<Node>();

  // send query byte, appended with the group id
  byte[] data = new byte[ 5 ];
  data[ 0 ] = QUERY_PACKET;
  encode( group, data, 1 );

  DatagramPacket tx = new DatagramPacket( data, data.length, broadcastAddress );

  bcastSocket.send( tx );

  // wait for the listen thread to do its thing
  try
  {
    Thread.sleep( timeout );
  }
  catch( InterruptedException e )
  {
	  e.printStackTrace();
  }

  Node [] nodes;
  synchronized( responseList )
  {
    nodes = responseList.toArray( new Node[ responseList.size() ] );
  }

  responseList = null;

  return nodes;
}

/**
 * Record of a node
 * 
 * @author Sumeet
 */
public class Node
{
  /**
   * The ip of the node
   */
  public final InetAddress ip;

  /**
   * The data of the node
   */
  public final int data;

  private Node( InetAddress ip, int data )
  {
    this.ip = ip;
    this.data = data;
  }

  @Override
  public String toString()
  {
    return ip.getHostAddress() + " " + data;
  }
}

/**
 * Handles an exception.
 * 
 * @author Sumeet
 */
public class ExceptionHandler
{
  /**
   * Called whenever an exception is thrown from the listen
   * thread. The listen thread should now be dead
   * 
   * @param e
   */
  public void handle( Exception e )
  {
    e.printStackTrace();
  }
}

/**
 * @param args
 */
public static void main( String[] args )
{
  try
  {
    int group = 6969;

    NodeSensing mp = new NodeSensing( group, 6969 );

    boolean stop = false;

    BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );

    while( !stop )
    {
      System.out.println( "enter \"q\" to quit, or anything else to query nodes" );
      String s = br.readLine();

      if( s.equals( "q" ) )
      {
        System.out.print( "Closing down..." );
        mp.disconnect();
        System.out.println( " done" );
        stop = true;
      }
      else
      {
        System.out.println( "Querying" );

        Node[] nodes = mp.getNodes( 100, ( byte ) 0 );

        System.out.println( nodes.length + " nodes found" );
        for( Node p : nodes )
        {
          System.out.println( "\t" + p );
        }
      }
    }
  }
  catch( Exception e )
  {
    e.printStackTrace();
  }
}

private static int decode( byte[] b, int index )
{
  int i = 0;

  i |= b[ index ] << 24;
  i |= b[ index + 1 ] << 16;
  i |= b[ index + 2 ] << 8;
  i |= b[ index + 3 ];

  return i;
}

private static void encode( int i, byte[] b, int index )
{
  b[ index ] = ( byte ) ( i >> 24 & 0xff );
  b[ index + 1 ] = ( byte ) ( i >> 16 & 0xff );
  b[ index + 2 ] = ( byte ) ( i >> 8 & 0xff );
  b[ index + 3 ] = ( byte ) ( i & 0xff );
}
}