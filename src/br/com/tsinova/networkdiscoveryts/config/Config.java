package br.com.tsinova.networkdiscoveryts.config;

import java.util.List;

public class Config {
    
    private Output output;
    private Beat beat;
    private List<String> switchs;
    private List<SnmpCommunity> snmpCommunities;

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Beat getBeat() {
        return beat;
    }

    public void setBeat(Beat beat) {
        this.beat = beat;
    }

    public List<String> getSwitchs() {
        return switchs;
    }

    public void setSwitchs(List<String> switchs) {
        this.switchs = switchs;
    }

    public List<SnmpCommunity> getSnmpCommunities() {
        return snmpCommunities;
    }

    public void setSnmpCommunities(List<SnmpCommunity> snmpCommunities) {
        this.snmpCommunities = snmpCommunities;
    }
    
}