package com.yarmis.core;

import com.yarmis.core.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Collection;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.FileInputStream;

/**
 * Instances of this class are used to associate known devices with
 * a set of permissions. These permissions determine whether or not
 * a specific device may call a function remotely.
 *
 * Instance of this class can be backed by a file, in which case the
 * repository will be populated initially by the contents of that file, 
 * and all changes to the set of known permissions and devices are written
 * back to that file.
 *
 * Note that this file is re-written after every change. In case there
 * are many (i.e. thousands) of known devices, it might be more efficient
 * to write the content of this repository to a file only at specific times.
 * 
 */
public class PermissionRepository {

    // The mapping that contains the known permissions for all known devices
    private final HashMap<Device, ArrayList<String>> repository;

    // Whether this repository is backed by a file
    private boolean isBacked = false;
    // The file that backs this repository, if any
    private File file = null;

    private static final String DEVICE = "device";
    private static final String PERMISSIONS = "permissions";

    /**
     * Creates a new PermissionRepository that is populated
     * with the entries of the given JSONArray.
     * 
     * @param  a The JSONArray that is used to populate the repository.
     */
    public PermissionRepository(JSONArray a) throws JSONException {
        this();
        populate(a);
    }

    /**
     * Creates a new PermissionRepository that is backed by a File.
     * The repository will be populated by the current content of this file,
     * and all changes to this repository will be written back to the file
     * immediately.
     * 
     * @param  f The file that is backing this PermissionRepository
     */
    public PermissionRepository(File f) throws IOException, JSONException {
        this();
        setBackingFile(f);
    }

    /**
     * Creates a new, empty PermissionRepository.
     */
    public PermissionRepository() {
        this.repository = new HashMap<Device, ArrayList<String>>();
    }

    /**
     * Populates this repository with the content of the given JSONArray.
     * 
     * @param  a The JSONArray that is read from to populate this repository
     */
    public void populate(JSONArray a) throws JSONException {
        repository.clear();
        if(a == null) return;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            Device device = Device.fromJSON(o.getJSONObject(DEVICE));
            ArrayList<String> permissions = new ArrayList<String>();
            JSONArray p = o.getJSONArray(PERMISSIONS);
            for (int j = 0; j < p.length(); j++) {
                permissions.add(p.getString(j));
            }
            repository.put(device, permissions);
        }
    }

    /**
     * Populates this repository with the current content
     * of the given file.
     * 
     * @param  f The file that is read from to populate this repository
     */
    public void populate(File f) throws IOException, JSONException {
        repository.clear();
        if(!f.exists()) return;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
        Scanner s = new Scanner(is).useDelimiter("\\A");
        JSONArray a = s.hasNext() ? new JSONArray(s.next()) : new JSONArray();
        populate(a);
        is.close();
    }

    /**
     * Specifies the file that backs this PermissionRepository.
     * When changing the backing file, this repository will be emptied
     * and re-populated from the content of the file.
     * All future changes to the content of this repository will be
     * written to the backing file immediately.
     *
     * Users who only want to populate this repository with the
     * content of a specific file should call the corresponding
     * populate function.
     * 
     * @param  f The file that is backing this PermissionRepository
     */
    public void setBackingFile(File f) throws IOException, JSONException {
        this.file = f;
        if(f == null) {
            isBacked = false;
        } else {
            isBacked = true;
            populate(f);
        }
    }

    /**
     * Writes the entries of this repository to the given file.
     * @param  f The file to write to
     */
    public void writeToFile(File f) throws IOException, JSONException {
        JSONArray a = toJSONArray();
        BufferedWriter os = new BufferedWriter(new FileWriter(f));
        try {
            os.write(a.toString());
        } catch(IOException e) {
            throw new IOException("Unable to write to file " + f, e);
        } finally {
            os.close();
        }
    }

    /**
     * Converts the entries of this repository to a JSONArray.
     */
    public JSONArray toJSONArray() throws JSONException {
        JSONArray a = new JSONArray();
        Set<Entry<Device, ArrayList<String>>> entries = repository.entrySet();
        for (Entry<Device, ArrayList<String>> e : entries) {
            JSONObject o = new JSONObject();
            o.put(DEVICE, ((Device) e.getKey()).toJSON());
            o.put(PERMISSIONS, e.getValue());
            a.put(o);
        }
        return a;
    }

    /**
     * Determines whether this repository contains an entry for the given device.
     */
    public boolean isKnown(Device d) {
        return repository.containsKey(d);
    }

    /**
     * Adds the given permission for the given device
     */
    public void addPermission(Device d, String... permissions) {
        if(permissions.length == 0) return;

        ArrayList<String> currentPermissions;
        if(!isKnown(d)) {
            currentPermissions = new ArrayList<String>();
            // Only in this case we need to put a new entry into the map
            repository.put(d, currentPermissions);
        } else {
            currentPermissions = repository.get(d);
        }

        for (String s : permissions) {
            if(currentPermissions.contains(s)) continue;
            currentPermissions.add(s);
        }

        try {
            if(isBacked) writeToFile(file);
        } catch(IOException | JSONException e) {
            // Print error, but proceed
            Log.e("Permissions", e);
        }
    }

    /**
     * Remove the given permission for the given device
     */
    public void removePermission(Device d, String... permissions) {
        if(!isKnown(d)) return;
        if(permissions.length == 0) return;

        ArrayList<String> currentPermissions = repository.get(d);
        for (String s : permissions) {
            currentPermissions.remove(s);
        }

        // clean up unused keys
        if(currentPermissions.isEmpty()) {
            repository.remove(d);
        }

        try {
            if(isBacked) writeToFile(file);
        } catch(IOException | JSONException e) {
            // Print error, but proceed
            e.printStackTrace();
        }
    }

    /**
     * Returns the set of permissions for a given device.
     * Note that changes to the returned array are not reflected
     * in this repository.
     *
     * If the given device is not known, an empty list will be returned.
     */
    public ArrayList<String> getPermissions(Device d) {
        ArrayList<String> permissions = repository.get(d);
        if(permissions == null) {
            return new ArrayList<String>();
        } else {
            return new ArrayList<String>(permissions);
        }
    }
}