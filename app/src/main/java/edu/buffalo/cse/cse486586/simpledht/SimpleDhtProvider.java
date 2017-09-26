package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.R.attr.key;
import static java.lang.Integer.parseInt;


class Node
{
    HashMap<String, String> data;
    String next, prev;
    String Node_id;
    int propogator;

    /* Constructor */
    public Node()
    {
        next = null;
        prev = null;
        data = null;
        Node_id = null;
        propogator = 0;
    }
    /* Constructor */
    public Node(HashMap d, String n, String p, String id, int index)
    {
        data = d;
        next = n;
        prev = p;
        Node_id = id;
        propogator = index;

    }
    /* Function to set link to next node */
    public void setLinkNext(String n)
    {
        next = n;
    }
    /* Function to set link to previous node */
    public void setLinkPrev(String p)
    {
        prev = p;
    }
    /* Funtion to get link to next node */
    public String getLinkNext()
    {
        return next;
    }
    /* Function to get link to previous node */
    public String getLinkPrev()
    {
        return prev;
    }
    /* Function to set data to node */
    public void setData(HashMap d)
    {
        data = d;
    }
    /* Function to get data from node */
    public HashMap<String, String> getData()
    {
        return data;
    }

    public void setNodeid(String id){
        Node_id = id;
    }

    public String getNodeid(){
        return Node_id;
    }
    public void set_propogator(int index){
        propogator = index;
    }
    public int get_propogator(){
        return propogator;
    }
}


public class SimpleDhtProvider extends ContentProvider {

    static final String TAG = "Content Provider";
    public int ServerPort = 10000;
    static HashMap<String, String> hm;
    static ArrayList<String> chord_names = new ArrayList<String>();
    private boolean reply = false;
    Node n = new Node();
    Cursor cursor= null;
    String result;
    private String DELIMITER = "@#@#@";
    private String DELIMITER2 = "@@@";



    private String hashMapToString(HashMap<String,String> hashMap){
        String hashMapStr = "";

        if(hashMap == null){
            return  hashMapStr;
        }


        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            hashMapStr += (entry.getKey()+DELIMITER+entry.getValue()+DELIMITER2);
        }
        return hashMapStr;
    }


    private HashMap<String,String> stringToHashMap(String strHashMap){
        HashMap<String ,String > newHashMap = new HashMap<String, String>();

        if(strHashMap == null || strHashMap.length()==0){
            return  null;
        }

        String hashMapKeyValuePairs[] = strHashMap.split(DELIMITER2);

        //Log.d(TAG,"LOG0 : "+strHashMap);

        for(int i = 0; i < hashMapKeyValuePairs.length; i++){
            if(hashMapKeyValuePairs[i]!=null && hashMapKeyValuePairs[i].trim().length()!=0 && !hashMapKeyValuePairs[i].equals("")) {
                String keyVal[] = hashMapKeyValuePairs[i].split(DELIMITER);
                //Log.d(TAG,"LOG1 :"+hashMapKeyValuePairs[i]+"HELLO");
                //Log.d(TAG,"LOG2 : "+ keyVal[0] + " : "+ keyVal[1]);
                String key = keyVal[0];
                String value = keyVal[1];
                newHashMap.put(key, value);
            }
        }
        return newHashMap;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        String[] selection_arr = selection.split("##");
        String delete_string = "";
        if(selection_arr.length==1){
            delete_string = selection_arr[0];
        }
        else {
            if (selection_arr[2].equals(String.valueOf(Integer.parseInt(get_port())/2)))
                return 1;
            delete_string = selection_arr[1];
        }




        if(delete_string.equals("@")){
            n.data.clear();
            return 1;
        }
        else{
            if(delete_string.equals("*")){
                n.data.clear();
                String del_message = "";
                if (selection_arr.length==1)
                {

                    del_message  = "DELETE##"+delete_string+"##"+String.valueOf(Integer.parseInt(get_port())/2)+"##"+n.next;
                }
                else {
                    del_message = "DELETE##" + delete_string + "##" + selection_arr[2]+"##"+n.next;
                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,del_message);
            }
            else if(n.data.isEmpty()) {
                String del_message = "";
                if (selection_arr.length == 1) {

                    del_message = "DELETE##" + delete_string + "##" + String.valueOf(Integer.parseInt(get_port()) / 2) + "##" + n.next;
                } else {
                    del_message = "DELETE##" + delete_string + "##" + selection_arr[2] + "##" + n.next;
                }
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, del_message);
            }else {
                if (n.data.get(delete_string) != null) {
                    n.data.remove(delete_string);
                    return 1;
                } else {
                    String del_message = "";
                    if (selection_arr.length == 1) {

                        del_message = "DELETE##" + delete_string + "##" + String.valueOf(Integer.parseInt(get_port()) / 2) + "##" + n.next;
                    } else {
                        del_message = "DELETE##" + delete_string + "##" + selection_arr[2] + "##" + n.next;
                    }
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, del_message);
                }
            }
        }


        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        try {
            String key_hash = genHash(values.get("key").toString());
            Log.d(TAG, "key_hash:  "+key_hash+"key:   "+values.get("key").toString());
            if(n.next==null||n.prev==null||genHash(n.next).equals(n.getNodeid())&&genHash(n.prev).equals(n.getNodeid())){
                Log.d(TAG,"");
                hm.put(values.get("key").toString(), values.get("value").toString());
                Log.d(TAG,"In insert hm"+ hm);
            }
            else if(key_hash.compareTo(genHash(n.prev))>0&&key_hash.compareTo(n.getNodeid())>=0&&n.getNodeid().compareTo(genHash(n.prev))<0){
                Log.d(TAG, "In special case for greater");

                Log.d(TAG, "n.getNodeid(): "+n.getNodeid());
                Log.d(TAG, "genHash(n.prev):  "+genHash(n.prev));
                Log.d(TAG, "key_hash:   "+key_hash);
                hm.put(values.get("key").toString(), values.get("value").toString());
                Log.d(TAG,"In insert hm"+ hm);
            }
            else if(key_hash.compareTo(genHash(n.prev))<0&&key_hash.compareTo(n.getNodeid())<=0&&n.getNodeid().compareTo(genHash(n.prev))<0){

                Log.d(TAG, "In special case for smaller");
                Log.d(TAG, "n.getNodeid(): "+n.getNodeid());
                Log.d(TAG, "genHash(n.prev):  "+genHash(n.prev));
                Log.d(TAG, "key_hash:   "+key_hash);

                hm.put(values.get("key").toString(), values.get("value").toString());
                Log.d(TAG,"In insert hm"+ hm);
            }
            else if(key_hash.compareTo(n.getNodeid())>0){
                String insert_msg = "INSERT##"+n.next+"##"+values.get("key").toString()+"##"+values.get("value").toString();
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,insert_msg);

            }
            else if(key_hash.compareTo(n.getNodeid())<=0&&key_hash.compareTo(genHash(n.prev))>0){

                Log.d(TAG, "In the just right case");
                Log.d(TAG, "n.getNodeid(): "+n.getNodeid());
                Log.d(TAG, "genHash(n.prev):  "+genHash(n.prev));

                Log.d(TAG, "key_hash:   "+key_hash);
                hm.put(values.get("key").toString(), values.get("value").toString());
                Log.d(TAG,"In insert hm"+ hm);
            }
            else{
                String insert_msg = "INSERT##"+n.next+"##"+values.get("key").toString()+"##"+values.get("value").toString();
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,insert_msg);
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG,"Inside onCreate");

        try{
            ServerSocket serverSocket = new ServerSocket(ServerPort);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }


        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"JOIN");

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        String[] selection_arr = selection.split("##");

        Log.v(TAG,"In query wih status:"+selection_arr[0]+" cursor:"+cursor+" reply:"+reply);
        // TODO Auto-generated method stub
        try {
            if(selection.equals("*")){
                if(n.next==null||n.prev==null||genHash(n.next).equals(n.getNodeid())&&genHash(n.prev).equals(n.getNodeid())){
                    cursor = null;
                    String[] str = new String[] {"key","value"};
                    MatrixCursor cur = new MatrixCursor(str);

                    for (Map.Entry<String, String> entry : hm.entrySet()) {
                        String k = entry.getKey();
                        String val = entry.getValue();
                        Log.d(TAG,"k:   "+k+"Val:  "+val);
                        String[] curadd = new String[] {k,val};
                        cur.addRow(curadd);
                    }
                    return cur;
                }
                else{

                    String hashmap_string = hashMapToString(n.data);
                    Log.d(TAG, "hashmap_string:  "+hashmap_string);
                    Log.d(TAG,"Selection:  "+selection);
                    Log.d(TAG,"Selection:  "+selection_arr.length);
                    String message_to_send = "";
                    message_to_send = "STAR##"+Integer.parseInt(get_port())/2+"##"+n.next+"##"+hashmap_string;
                    Log.d(TAG,"_______________________________");
                    Log.d(TAG,message_to_send);
                    Log.d(TAG,"_______________________________");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message_to_send);
                    result = new String();

                    while (true){
                        if(reply){
                            break;
                        }
                    }

                }
            }
            else if(selection.equals("@")){
                cursor = null;
                String[] str = new String[] {"key","value"};
                MatrixCursor cur = new MatrixCursor(str);

                for (Map.Entry<String, String> entry : hm.entrySet()) {
                    String k = entry.getKey();
                    String val = entry.getValue();
                    Log.d(TAG,"k:   "+k+"Val:  "+val);
                    String[] curadd = new String[] {k,val};
                    cur.addRow(curadd);
                }
                return cur;
            }
            else if(selection_arr[0].equals("STAR")){
                int port = Integer.parseInt(get_port())/2;

                if(String.valueOf(port).equals(selection_arr[1])){
                    cursor = null;
                    result = selection_arr[3];
                    String[] str = new String[] {"key","value"};
                    MatrixCursor cur = new MatrixCursor(str);
                    HashMap<String, String> h = stringToHashMap(result);
                    for (Map.Entry<String, String> entry : h.entrySet()) {
                        String[] curadd = new String[] {entry.getKey(),entry.getValue()};
                        cur.addRow(curadd);
                    }
                    cursor = cur;
                    reply= true;
                    return cur;

                }
                else{
                    String hashmap_string = hashMapToString(n.data);
                    Log.d(TAG, "hashmap_string:  "+hashmap_string);
                    Log.d(TAG,"Selection:  "+selection);
                    Log.d(TAG,"Selection:  "+selection_arr.length);
                    String message_to_send = "";
                    if(selection_arr.length == 3){
                        message_to_send = "STAR##"+selection_arr[1]+"##"+n.next+"##"+hashmap_string;
                    }else{
                        message_to_send = "STAR##"+selection_arr[1]+"##"+n.next+"##"+selection_arr[3]+DELIMITER2+hashmap_string;
                    }

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message_to_send);
                }

            }
            else{
                String[] str = new String[] {"key","value"};
                MatrixCursor cur = new MatrixCursor(str);
                Log.d(TAG, "In Query else");
                String value="";

                if(selection_arr.length==1){
                    cursor = null;
                    Log.d(TAG, "selection:  "+selection);
                    int port = Integer.parseInt(get_port())/2;
                    if((value=n.data.get(selection))!=null){
                        Log.d(TAG,"Value found at first");
                        String[] curadd = new String[] {selection,value};
                        cur.addRow(curadd);
                        return cur;
                    }
                    else{
                        Log.d(TAG,"Value not found at first. Propagating to :  "+n.next);
                        String query_message = "QUERY##"+selection+"##"+String.valueOf(port)+"##"+n.next;
                        Log.d(TAG,"Sending query:  "+query_message);
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,query_message);
                        while (true){
                            if(reply){
                                break;
                            }
                        }
                    }
                }
                else if(selection_arr.length>1){
                    if(selection_arr[0].equals("QUERY")){
                        Log.d(TAG,"In server:  "+get_port()+" with query:  "+selection);
                        if((value=n.data.get(selection_arr[1]))!=null){
                            Log.d(TAG,"Value found in some other server");
                            String query_message = "RESPONSE##"+selection_arr[1]+"##"+selection_arr[2]+"##"+value;
                            Log.d(TAG,"Sending response:  "+query_message);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,query_message);
                         /*   while (true){
                                if(reply){
                                    break;
                                }
                            }*/
                        }
                        else{
                            Log.d(TAG,"Value not found in this server.. sending response to next server");
                            String query_message = "QUERY##"+selection_arr[1]+"##"+selection_arr[2]+"##"+n.next;
                            Log.d(TAG,"Sending query:  "+query_message);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,query_message);
                       /*     while (true){
                                if(reply){
                                    break;
                                }
                            }*/
                        }
                    }

                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
            cursor = null;
            reply = true;
            return cursor;
        }catch (Exception e) {
            e.printStackTrace();
        }
        if(selection_arr[0].equals("RESPONSE")){
            String[] str = new String[] {"key","value"};
            MatrixCursor cur = new MatrixCursor(str);
            Log.d(TAG,"Response found! "+selection);
            String[] curadd = new String[] {selection_arr[1],selection_arr[3]};
            Log.d(TAG, "curadd   :"+curadd[0]+"   "+curadd[1]);
            cur.addRow(curadd);
            cursor = cur;
            return cur;
        }
        Log.v(TAG,"I query bfore return NuLL");
        if (reply) {
            reply = false;
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }






    public String get_port(){
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((parseInt(portStr) * 2));
        return myPort;
    }





    public String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                while(true){

                    Socket socket = serverSocket.accept();
                    Log.d(TAG,"In server");
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    String message_in = in.readUTF();
                    Log.d(TAG,"Server_message: "+message_in);
                    String[] msgs = message_in.split("##");
                    if(msgs[0].equals("JOIN")){
                        String curr_hash = genHash(msgs[1]);
                        int index = (parseInt(msgs[1])%11108)/4;
                        Log.d(TAG, "curr_hash:  "+curr_hash);
                        chord_names.add(msgs[1]);
                        Collections.sort(chord_names, new HashComparator());
                        int sorted_index = chord_names.indexOf(msgs[1]);
                        Log.d(TAG,"sorted_index:  "+sorted_index);
                        String prev_link = null;
                        String next_link = null;
                        if(sorted_index+1>chord_names.size()-1){
                            next_link = chord_names.get(0);
                        }
                        else
                            next_link = chord_names.get(sorted_index+1);
                        if(sorted_index-1<0){
                            prev_link = chord_names.get(chord_names.size()-1);
                        }
                        else
                            prev_link = chord_names.get(sorted_index-1);
                        if(0<sorted_index&&sorted_index<chord_names.size()-1){
                            prev_link = chord_names.get(sorted_index-1);
                            next_link = chord_names.get(sorted_index+1);
                        }
                        Log.d(TAG,"prev_link:  "+prev_link);
                        Log.d(TAG,"next_link: "+next_link);
                        Log.d(TAG, "Chord names:  "+chord_names);

                        int prev = Integer.parseInt(prev_link);
                        int next = Integer.parseInt(next_link);
                        Log.d(TAG,"In 3");

                        String message_to_send2 = "UPDATE##"+"abc"+"##"+msgs[1]+"##"+prev;
                        //noinspection WrongThread
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message_to_send2);

                        String message_to_send3 = "UPDATE##"+msgs[1]+"##"+"abc"+"##"+next;
                        //noinspection WrongThread
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message_to_send3);

                        String message_to_send1 = "UPDATE##"+prev_link+"##"+next_link+"##"+msgs[1];
                        //noinspection WrongThread
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message_to_send1);
                    }

                    else if(msgs[0].equals("UPDATE")){
                        Log.d(TAG,"In Server Update");
                        String prev_link = msgs[1];
                        String next_link = msgs[2];
                        Log.d(TAG, "prev_link: "+prev_link);
                        Log.d(TAG, "next_link: "+next_link);
                        Log.d(TAG,"Node next bef:  "+n.getLinkNext());
                        Log.d(TAG,"Node prev bef:  "+n.getLinkPrev());
                        if(!prev_link.equals("abc")){
                            n.setLinkPrev(prev_link);
                        }
                        if(!next_link.equals("abc")){
                            n.setLinkNext(next_link);
                        }
                        Log.d(TAG,"Node id:  "+n.getNodeid());
                        Log.d(TAG,"Node next after:  "+n.getLinkNext());
                        Log.d(TAG,"Node prev after:  "+n.getLinkPrev());
                        Log.d(TAG,"Node index:  "+n.get_propogator());
                        Log.d(TAG,"Node data:  "+n.getData());
                    }

                    else if(msgs[0].equals("INSERT")){
                        Log.d(TAG,"In Server Insert");
                        Log.d(TAG, "In Server Insert msgs rcvd:  "+message_in);
                        ContentValues cv = new ContentValues();
                        cv.put("key", msgs[2]);
                        cv.put("value", msgs[3]);
                            Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        insert(uri,cv);
                    }
                    else if(msgs[0].equals("QUERY")){
                        Log.d(TAG,"In Server Query");
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                   //     reply = false;
                        query(uri, null, message_in, null, null);
                    }
                    else if(msgs[0].equals("RESPONSE")){
                        Log.d(TAG,"In Server Response");
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                       // reply = false;

                        query(uri, null, message_in, null, null);
                        reply = true;
                    }
                    else if(msgs[0].equals("STAR")){
                        Log.d(TAG,"In Server Star");
                        Log.d(TAG, "Message:  "+message_in);
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        query(uri, null, message_in, null, null);
                    }
                    else if(msgs[0].equals("DELETE")){
                        Log.d(TAG,"In Server Delete");
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        delete(uri, message_in, null);
                        //socket.close();
                    }


                    /*in.close();
                    out.close();*/
                    //in.close();
                    //socket.close();
                }
            }
            catch(Exception e){
                Log.e(TAG,e.toString());
            }
            return null;
        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String[] client_message = msgs[0].split("##");

                if(client_message[0].equals("JOIN")){
                    Log.d(TAG, "In Client Join");
                    int port_number = parseInt(get_port());
                    //int index = (port_number%11108)/4;
                    int emulator_no = port_number/2;
                    hm = new HashMap<String, String>();
                    n.setData(hm);
                    n.set_propogator(0);
                    n.setLinkNext(null);
                    n.setLinkPrev(null);
                    n.setNodeid(genHash(String.valueOf(emulator_no)));
                    Log.d(TAG,"Port Number:  "+String.valueOf(emulator_no));
                    Log.d(TAG,"Node id:  "+n.getNodeid());
                    Log.d(TAG,"Node next:  "+n.getLinkNext());
                    Log.d(TAG,"Node prev:  "+n.getLinkPrev());
                    Log.d(TAG,"Node propogator:  "+n.get_propogator());
                    Log.d(TAG,"Node data:  "+n.getData());
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt("11108"));
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    String message_to_send ="JOIN##"+String.valueOf(emulator_no);
                    Log.d(TAG,"Client Message:  "+message_to_send);
                    out.writeUTF(message_to_send);
                    //out.close();
                    //socket.close();
                    //in.close();

                }
                else if(client_message[0].equals("UPDATE")){
                    Log.d(TAG,"In Client Update");
                    String port = client_message[3];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                    //socket.close();
                    //out.close();
                }
                else if(client_message[0].equals("INSERT")){
                    Log.d(TAG,"In Client Insert");
                    Log.d(TAG, "In Client Insert msgs rcvd:  "+msgs[0]);
                    String port = client_message[1];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                }
                else if(client_message[0].equals("QUERY")){
                    Log.d(TAG,"In Client Query");
                    String port = client_message[3];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                }
                else if(client_message[0].equals("RESPONSE")){
                    Log.d(TAG,"In Client Response");
                    String port = client_message[2];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                }
                else if(client_message[0].equals("STAR")){
                    Log.d(TAG,"In Client Star");
                    Log.d(TAG, "Message:  "+msgs[0]);
                    String port = client_message[2];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                }
                else if(client_message[0].equals("DELETE")){
                    Log.d(TAG,"In Client Delete");
                    String port = client_message[2];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port)*2);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgs[0]);
                }




            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class HashComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
            String gen_x = null;
            String gen_y = null;
            try {
                gen_x = genHash(x);
                gen_y = genHash(y);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return gen_x.compareTo(gen_y);
        }
    }

}



