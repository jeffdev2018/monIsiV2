package com.monisi.jeff.monisi;



import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HandlerFileReadWrite {

    private final static String TAG = "BLEDiscovery";

    private boolean is_read;
    private File fileToOpen = null;
    private FileWriter fileWriter;
    private FileReader fileReader;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    private Context context;

    public HandlerFileReadWrite(Context context) {
        this.context = context;
    }

    public void open(String fileName) {
        open(fileName, fileOperator.OPEN_READ);
    }

    public void open(String fileName, fileOperator op) {
        //fileToOpen = new File(context.getFilesDir(), fileName);
        File fileDir[] = context.getExternalMediaDirs();
        if (fileDir.length > 1) {
            //sd card presents
            fileToOpen = new File(fileDir[1], fileName);
        } else {
            //only internal storage
            fileToOpen = new File(fileDir[0], fileName);
        }
        //Log.i(TAG, "FILE PATH " + fileToOpen.toString() );
        switch (op) {
            case OPEN_READ: {
                try {
                    fileReader = new FileReader(fileToOpen);
                    bufferedReader = new BufferedReader(fileReader);
                    is_read = true;
                } catch (IOException e) {
                    //
                }
                break;
            }
            case OPEN_WRITE: {
                try {
                    fileWriter = new FileWriter(fileToOpen, false);
                    bufferedWriter = new BufferedWriter(fileWriter);
                    is_read = false;
                } catch (IOException e) {
                    //
                }
                break;
            }
            case OPEN_APPEND: {
                try {
                    fileWriter = new FileWriter(fileToOpen, true);
                    bufferedWriter = new BufferedWriter(fileWriter);
                    is_read = false;
                } catch (IOException e) {
                    //
                }
                break;
            }
        }
    }

    public String readLine()
    {
        if (!is_read) {
            System.err.println("This object is not open to read");
            return null;
        }

        if (bufferedReader != null) {
            try {
                return bufferedReader.readLine();
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }


    public void writeLine(String data)
    {
        if (is_read) {
            System.err.println("This object is not open to write");
            return;
        }

        if (bufferedWriter != null) {
            try {
                bufferedWriter.write(data);
                bufferedWriter.write("\r\n");
            } catch (IOException e) {
                //
            }
        }
    }

    public void close() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
                fileWriter.close();
            } catch (IOException e) {
                //
            }
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public enum fileOperator {
        OPEN_READ, OPEN_WRITE, OPEN_APPEND;
    }
}