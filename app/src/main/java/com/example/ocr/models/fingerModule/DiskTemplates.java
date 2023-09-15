package com.example.ocr.models.fingerModule;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DiskTemplates {

    private static final String TAG = "DiskTemplates";

    public static final int TEMPLATE_LENGTH = 1024;
    public static final int MAX_FINGER_COUNT = 10000;

    private final int COUNT = MAX_FINGER_COUNT;
    private final byte[] templates = new byte[TEMPLATE_LENGTH * COUNT];
    private String dirPath;
    private final ArrayList<String> nameList = new ArrayList<>();


    synchronized int refreshTemplatesFromFile(String filePath) {
        this.dirPath = filePath;
        File file = new File(filePath);
        File[] files = file.listFiles();
        if (files == null) {
            return 0;
        }
        nameList.clear();
        Arrays.fill(templates, (byte) 0);
        int total = Math.min(MAX_FINGER_COUNT,files.length);
        for (int i = 0; i < total; i++) {
            nameList.add(files[i].getName());
            byte[] data = readFile(files[i]);
            System.arraycopy(data, 0, templates, i * TEMPLATE_LENGTH, data.length);
        }
        return total;

    }

    public synchronized byte[] getAll() {
        return templates;
    }

    public synchronized int count() {
        return nameList.size();
    }


    public synchronized boolean put(String name, byte[] data) {
        int size = nameList.size();
        boolean contains = nameList.contains(name);
        if (contains) {
            return false;
        }
        nameList.add(name);
        System.arraycopy(data, 0, templates, size * TEMPLATE_LENGTH, data.length);
        writeFile(name, data);
        return true;
    }

    /**
     *
     * @return true success , false - not found
     */
    public synchronized boolean delete(String name) {
        int index = nameList.indexOf(name);
        if (index == -1) {
            return false;
        }
        nameList.remove(index);
        System.arraycopy(templates, (index + 1) * TEMPLATE_LENGTH, templates, (index) * TEMPLATE_LENGTH, TEMPLATE_LENGTH);
        deleteFile(name);
        return true;
    }

    public synchronized byte[] get(String name) {
        int index = nameList.indexOf(name);

        if (index == -1) {
            return null;
        }
        byte[] data = new byte[TEMPLATE_LENGTH];
        System.arraycopy(templates, index * TEMPLATE_LENGTH, data, 0, TEMPLATE_LENGTH);
        return data;
    }

    public synchronized String getId(int index) {
        return nameList.get(index);
    }


    public synchronized String getIds() {
        String s = Arrays.toString(nameList.toArray());
        return s;
    }

    synchronized void clear() {
        Arrays.fill(templates, (byte) 0);
        nameList.clear();
        File[] files = new File(dirPath).listFiles();
        if (files != null && files.length != 0) {
            Log.w(TAG, "clear: WARNING . not null");
            for (File fi : files) {
                fi.delete();
            }
        }
    }

    private byte[] readFile(File file) {
        byte[] bytes = new byte[1024];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private void writeFile(String name, byte[] bytes) {
        File path = new File(dirPath, name);
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(String name) {
        File file = new File(dirPath, name);
        file.delete();
    }

}
