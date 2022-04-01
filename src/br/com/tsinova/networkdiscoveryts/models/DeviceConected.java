package br.com.tsinova.networkdiscoveryts.models;

public class DeviceConected {

    private String ip;
    private String mac;
    private String type;
    private String category;
    private String systems;
    private String os;
    private String vendor;
    private String description;

    public DeviceConected(String ip, String mac) {
        this.ip = ip;
        this.mac = mac;
    }

    public DeviceConected() {
    }
    
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSystems() {
        return systems;
    }

    public void setSystems(String systems) {
        this.systems = systems;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isSwitch() {
        return category != null && category.equalsIgnoreCase("switch");
    }

    @Override
    public String toString() {
        return "MacIp{" + "ip=" + ip + ", mac=" + mac + ", type=" + type + ", category=" + category + ", systems=" + systems + ", os=" + os + ", vendor=" + vendor + ", description=" + description + '}';
    }        
    
    
}
