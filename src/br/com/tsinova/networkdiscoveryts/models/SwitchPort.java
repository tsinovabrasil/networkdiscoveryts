package br.com.tsinova.networkdiscoveryts.models;

import java.util.ArrayList;
import java.util.List;

public class SwitchPort {

    private int index;
    private int indexRef;
    private String description;
    private List<DeviceConected> devices = new ArrayList<>();

    public SwitchPort(int port, List<DeviceConected> devices) {
        this.index = port;
        this.devices = devices;
    }

    public SwitchPort(int port) {
        this.index = port;
    }
    
    public SwitchPort() {
    }   
    
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndexRef() {
        return indexRef;
    }

    public void setIndexRef(int indexRef) {
        this.indexRef = indexRef;
    }   

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<DeviceConected> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceConected> devices) {
        this.devices = devices;
    }    
    
    public void addDevice(DeviceConected device){
        if (devices == null){
            devices = new ArrayList<>();
        }
        devices.add(device);
    }

    @Override
    public String toString() {
        return "SwitchPort{" + "index=" + index + ", indexRef=" + indexRef + ", description=" + description + '}';
    }    
    
}
