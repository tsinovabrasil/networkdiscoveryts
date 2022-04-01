package br.com.tsinova.networkdiscoveryts.config;

public class SnmpCommunity {
    
    private int port;
    private String community;
    private String version;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
}
