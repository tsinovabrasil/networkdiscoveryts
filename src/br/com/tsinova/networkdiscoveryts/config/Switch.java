package br.com.tsinova.networkdiscoveryts.config;

import br.com.tsinova.networkdiscoveryts.models.SwitchPort;
import java.util.ArrayList;
import java.util.List;

public class Switch {

    private String ip;
    private String mac;
    private List<SwitchPort> ports;
    private String description;

    public Switch(String ip) {
        this.ip = ip;
    }
    
    public Switch(String ip, String mac) {
        this.ip = ip;
        this.mac = mac;
        this.ports = new ArrayList<>();
        this.description = "";
    }

    public Switch(String ip, String mac, String description) {
        this.ip = ip;
        this.mac = mac;
        this.description = description;
        this.ports = new ArrayList<>();
    }        

    public Switch() {
        this.ports = new ArrayList<>();
        this.ip = "";
        this.mac = "";
        this.description = "";
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

    public List<SwitchPort> getPorts() {
        return ports;
    }

    public void setPorts(List<SwitchPort> ports) {
        this.ports = ports;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void addPort(SwitchPort port) {
        if (ports == null) {
            this.ports = new ArrayList<>();
        }
        this.ports.add(port);
    }
    
}
