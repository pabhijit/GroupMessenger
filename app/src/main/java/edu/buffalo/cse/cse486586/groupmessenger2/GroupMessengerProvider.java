package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String TAG = GroupMessengerProvider.class.getSimpleName();
    private static final Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    private static boolean cameNow = true;
    private static int delivered = 0;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        String filename = null;
        String content = null;
        FileOutputStream outputStream;

        try {
            for(String key: values.keySet()) {
                if(key.equals("key")){
                    filename = String.valueOf(values.get(key));
                } else {
                    content = String.valueOf(values.get(key));
                }
            }
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        String line = null;
        String[] cols = new String[] { "key", "value" };
        StringBuffer value = new StringBuffer();
        MatrixCursor cursor = new MatrixCursor(cols);
        try {

            if(cameNow) {
                for (Message entry :GroupMessengerActivity.deliveryQueue){
                    ContentValues keyValueToInsert = new ContentValues();

                    keyValueToInsert.put("key", delivered);
                    keyValueToInsert.put("value", entry.getMsg());

                    insert( providerUri, keyValueToInsert);
                    delivered = delivered + 1;
                }
            }
            cameNow = false;
            FileInputStream fis = getContext().openFileInput(selection);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            while ((line = reader.readLine()) != null)   {
                value.append(line);
            }
            String[] values = new String[]{selection, value.toString()};
            cursor.addRow(values);
            Log.v("query", selection);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return cursor;
    }

    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
