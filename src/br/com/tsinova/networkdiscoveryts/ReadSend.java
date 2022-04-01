package br.com.tsinova.networkdiscoveryts;

import br.com.tsinova.networkdiscoveryts.config.SnmpCommunity;
import br.com.tsinova.networkdiscoveryts.config.Switch;
import br.com.tsinova.networkdiscoveryts.config.OutputLogstash;
import br.com.tsinova.networkdiscoveryts.config.Config;
import br.com.tsinova.networkdiscoveryts.utils.Util;
import br.com.tsinova.networkdiscoveryts.models.DeviceConected;
import br.com.tsinova.networkdiscoveryts.models.SwitchPort;
import io.github.openunirest.http.HttpResponse;
import io.github.openunirest.http.JsonNode;
import io.github.openunirest.http.Unirest;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

public class ReadSend extends Thread {

    private final List<Switch> switchsByConfig;
    private final List<SnmpCommunity> communitiesByConfig;
    private final OutputLogstash output;
    private final Config config;
    private final boolean run;

    public ReadSend(Config config) {
        this.config = config;

        this.switchsByConfig = config.getSwitchs().stream().map(
                obj -> {
                    return new Switch(obj);
                }).collect(Collectors.toList());

        this.communitiesByConfig = config.getSnmpCommunities();

        this.output = config.getOutput().getLogstash();

        this.run = true;

    }

    private CommunityTarget getTarget(String ipDevice, SnmpCommunity host) {
        CommunityTarget target = new CommunityTarget();
        if (host.getVersion().equals("2c")) {
            target.setCommunity(new OctetString(host.getCommunity()));
            target.setAddress(GenericAddress.parse("udp:" + ipDevice + "/" + host.getPort()));
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);
        } else if (host.getVersion().equals("1")) {
            target.setCommunity(new OctetString(host.getCommunity()));
            target.setAddress(GenericAddress.parse("udp:" + ipDevice + "/" + host.getPort()));
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version1);
        }
        return target;
    }

    private CommunityTarget getTarget(String ipDevice, SnmpCommunity host, int timeOut, int retries) {
        CommunityTarget target = new CommunityTarget();
        if (host.getVersion().equals("2c")) {
            target.setCommunity(new OctetString(host.getCommunity()));
            target.setAddress(GenericAddress.parse("udp:" + ipDevice + "/" + host.getPort()));
            target.setRetries(retries);
            target.setTimeout(timeOut);
            target.setVersion(SnmpConstants.version2c);
        } else if (host.getVersion().equals("1")) {
            target.setCommunity(new OctetString(host.getCommunity()));
            target.setAddress(GenericAddress.parse("udp:" + ipDevice + "/" + host.getPort()));
            target.setRetries(retries);
            target.setTimeout(timeOut);
            target.setVersion(SnmpConstants.version1);
        }
        return target;
    }

    private boolean existsMibOnDevice(String ipDevice, SnmpCommunity snmpCommunity, String mib) {

        boolean existsMib = false;

        try {

            CommunityTarget target = getTarget(ipDevice, snmpCommunity, 800, 1);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            DefaultPDUFactory defaultPDUFactory = new DefaultPDUFactory(PDU.GETBULK);

            TreeUtils treeUtils = new TreeUtils(snmp, defaultPDUFactory);
            treeUtils.setIgnoreLexicographicOrder(!treeUtils.isIgnoreLexicographicOrder());

            List<TreeEvent> events = treeUtils.getSubtree(target, new OID("." + mib));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    existsMib = true;
                    break;
                }
                if (existsMib) {
                    break;
                }
            }

            snmp.close();
            transport.close();

        } catch (Exception ex) {
            existsMib = false;

        }

        return existsMib;

    }

    private boolean isNobreak(String ipDevice, List<SnmpCommunity> snmpCommunities) {

        boolean isNobreak = false;

        for (SnmpCommunity snmpCommunity : snmpCommunities) {
            isNobreak = existsMibOnDevice(ipDevice, snmpCommunity, "1.3.6.1.2.1.33");
            if (isNobreak) {
                break;
            }
        }

        return isNobreak;

    }

    private boolean isPrinter(String ipDevice, List<SnmpCommunity> snmpCommunities) {

        boolean isPrinter = false;

        for (SnmpCommunity snmpCommunity : snmpCommunities) {
            isPrinter = existsMibOnDevice(ipDevice, snmpCommunity, "1.3.6.1.2.1.43");
            if (isPrinter) {
                break;
            }
        }

        return isPrinter;

    }

    private boolean isSwitch(String ipDevice, List<SnmpCommunity> snmpCommunities) {

        boolean isSwitch = false;

        String service = getSysServices(ipDevice, snmpCommunities);

        if (service != null && !service.isEmpty()) {

            service = Integer.toBinaryString(Integer.parseInt(service));
            service = new StringBuilder(service).reverse().toString();

            if (service.startsWith("1", 1) || service.startsWith("1", 2)) {
                isSwitch = true;
            }

        }

        return isSwitch;

    }

    private boolean isServer(String ipDevice) {

        boolean isServer = false;

        try {

            String url = "https://" + ipDevice + "/redfish/v1/";
            HttpResponse<JsonNode> response = Unirest.get(url).asJson();

            if (response.getBody().getObject().has("Systems")) {
                isServer = true;
            }

        } catch (Exception ex) {
            isServer = false;
        }

        return isServer;

    }

    private String[] getTypeDevice(String ipDevice, List<SnmpCommunity> snmpCommunities) {

        try {

            boolean isDevice = isSwitch(ipDevice, snmpCommunities);
            if (isDevice) {
                return new String[]{null, "switch"};
            }

            isDevice = isPrinter(ipDevice, snmpCommunities);
            if (isDevice) {
                return new String[]{"impressora", "impressora"};
            }

            isDevice = isNobreak(ipDevice, snmpCommunities);
            if (isDevice) {
                return new String[]{"nobreak", "nobreak"};
            }

            isDevice = isServer(ipDevice);
            if (isDevice) {
                return new String[]{null, "servidor"};
            }

            return null;

        } catch (Exception ex) {
            return null;

        }

    }

    private String getResponse(CommunityTarget target, Snmp snmp, String oid, String add) throws Exception {

        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID("." + oid + add)));

        ResponseEvent responseEvent = snmp.send(pdu, target);
        PDU response = responseEvent.getResponse();

        for (int i = 0; i < response.size(); i++) {

            VariableBinding variableBinding = response.get(i);

            if (variableBinding.getOid().toString().equalsIgnoreCase(oid + add)) {

                if (variableBinding.isException() || variableBinding.getVariable().isException()
                        || !variableBinding.getOid().isValid() || variableBinding.getOid().isException()) {

                    throw new Exception("no results found for oid " + (oid + add));
                }

                return variableBinding.getVariable().toString();

            }

        }

        throw new Exception("no results found for oid " + (oid + add));

    }

    private String getSysServices(String ipDevice, SnmpCommunity community) {

        String oid = "1.3.6.1.2.1.1.7.0";

        TransportMapping transport = null;
        Snmp snmp = null;

        try {

            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = getTarget(ipDevice, community);

            return getResponse(target, snmp, oid, "");

        } catch (Exception ex) {
            return null;

        } finally {
            try {
                snmp.close();
                transport.close();
            } catch (Exception ex) {
            }

        }

    }

    private boolean checkCommunity(String ipDevice, SnmpCommunity community) {

        String oid = "1.3.6.1.2.1.1.7.0";

        TransportMapping transport = null;
        Snmp snmp = null;

        try {

            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = getTarget(ipDevice, community);

            return !getResponse(target, snmp, oid, "").isEmpty();

        } catch (Exception ex) {
            return false;

        } finally {
            try {
                snmp.close();
                transport.close();
            } catch (Exception ex) {
            }

        }

    }

    private SnmpCommunity checkCommunities(String ipDevice, List<SnmpCommunity> communities) {

        for (SnmpCommunity c : communities) {
            boolean check = checkCommunity(ipDevice, c);
            if (check) {
                return c;
            }

        }

        return null;
    }

    private String getSysServices(String ipDevice, List<SnmpCommunity> communities) {

        for (SnmpCommunity c : communities) {
            String service = getSysServices(ipDevice, c);
            if (service != null && !service.isEmpty()) {
                return service;
            }

        }

        return null;
    }

    private String getResponseOidDevice(String ipDevice, String oid, String add) throws Exception {

        for (SnmpCommunity community : communitiesByConfig) {

            TransportMapping transport = null;
            Snmp snmp = null;

            try {

                transport = new DefaultUdpTransportMapping();
                snmp = new Snmp(transport);
                transport.listen();

                CommunityTarget target = getTarget(ipDevice, community);

                return getResponse(target, snmp, oid, add);

            } catch (Exception ex) {
            } finally {
                try {
                    snmp.close();
                    transport.close();
                } catch (Exception ex) {
                }

            }

        }

        throw new Exception("no results found for oid " + (oid + add));

    }

    private String getPartByOid(String oid) {
        String parts[] = oid.split("\\.");
        String p1 = parts[(parts.length - 1) - 3];
        String p2 = parts[(parts.length - 1) - 2];
        String p3 = parts[(parts.length - 1) - 1];
        String p4 = parts[(parts.length - 1)];
        String ip = p1 + "." + p2 + "." + p3 + "." + p4;
        return ip;

    }

    private void setVendorDevice(DeviceConected device) {
        try {
            String vendor = Util.getVendorByMacAdrress(device.getMac());
            device.setVendor(vendor);
        } catch (Exception ex) {
            device.setVendor(null);
        }
    }

    private void setTypeDevice(DeviceConected device) {

        String systems = null, category = null, description = null;

        try {

            // pega o cache
            JSONObject cacheTypeDevice = Util.getTypeDevice(device.getMac());

            // se houver cache, resgata os valores
            if (cacheTypeDevice != null) {

                systems = (!cacheTypeDevice.has("systems") || cacheTypeDevice.get("systems") == JSONObject.NULL ? null : cacheTypeDevice.getString("systems"));
                category = (!cacheTypeDevice.has("category") || cacheTypeDevice.get("category") == JSONObject.NULL ? null : cacheTypeDevice.getString("category"));
                description = (!cacheTypeDevice.has("description") || cacheTypeDevice.get("description") == JSONObject.NULL ? null : cacheTypeDevice.getString("description"));

            } else {

                // se não houver cache, busca via snmp
                String type[] = getTypeDevice(device.getIp(), communitiesByConfig);

                if (type != null) {

                    systems = type[0];
                    category = type[1];

                    if (category != null && category.equalsIgnoreCase("switch")) {
                        try {
                            description = getResponseOidDevice(device.getIp(), "1.3.6.1.2.1.1.1.0", "");
                        } catch (Exception ex) {
                        }
                    }

                }

                Util.saveTypeDevice(device.getMac(), category, systems, description);

            }

        } catch (Exception ex) {
        }

        device.setSystems(systems);
        device.setCategory(category);
        device.setDescription(description);

    }

    public void pushVendorAndTypeDevice(List<DeviceConected> list) {
        list.stream().forEach(obj -> {
            setTypeDevice(obj);
            setVendorDevice(obj);
        });
    }

    /**
     * Map<OID, MAC>
     *
     * @param switchIp
     * @return
     */
    public Map<String, String> getDevicesConectedBySwitch(String switchIp) {

        Map<String, String> list = new HashMap<>();

        try {

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.17.4.3.1.1"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {

                        String part = getPartByOid(varBinding.getOid().toString());
                        String mac = varBinding.getVariable().toString();
                        list.put(part, mac);

                    } catch (Exception ex) {
                    }

                }

            }

            snmp.close();
            transport.close();

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    public List<SwitchPort> getPortsAndDevicesConected(String switchIp, List<DeviceConected> devices) {

        List<SwitchPort> list = new ArrayList<>();

        try {

            list = getPortsBySwitch(switchIp);

            // Map<OID, MAC>
            Map<String, String> listDevicesConectedBySwitch = getDevicesConectedBySwitch(switchIp);

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.17.4.3.1.2"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {

                        String part = getPartByOid(varBinding.getOid().toString());
                        int port = varBinding.getVariable().toInt();

                        if (listDevicesConectedBySwitch.containsKey(part)) {

                            String mac = listDevicesConectedBySwitch.get(part);
                            DeviceConected device = devices.stream().filter(obj -> obj.getMac().equalsIgnoreCase(mac)).findFirst().orElse(null);

                            if (device == null) {
                                device = new DeviceConected();
                                device.setMac(mac);
                            }

                            SwitchPort switchPort = list
                                    .stream()
                                    .filter(obj -> obj.getIndex() == port || obj.getIndexRef() == port)
                                    .findFirst().orElse(null);

                            if (switchPort != null) {
                                switchPort.addDevice(device);
                            }

                        }

                    } catch (Exception ex) {
                    }

                }

            }

            snmp.close();
            transport.close();

        } catch (Exception ex) {
        }

        list = list.stream().filter(obj -> obj.getDevices().size() > 0).collect(Collectors.toList());

        return list;

    }

    public List<SwitchPort> getPortsBySwitch(String switchIp) {

        List<SwitchPort> list = new ArrayList<>();

        try {

            List<Integer> index = getPortIndexBySwitch(switchIp);
            Map<Integer, String> descriptions = getIndexDescriptionBySwitch(switchIp);
            Map<Integer, Integer> indexRef = getPortIndexRefBySwitch(switchIp);

            index.forEach(idx -> {

                SwitchPort switchPort = new SwitchPort();
                switchPort.setIndex(idx);
                if (descriptions.containsKey(idx)) {
                    switchPort.setDescription(descriptions.get(idx));
                }
                if (indexRef.containsKey(idx)) {
                    switchPort.setIndexRef(indexRef.get(idx));
                }
                list.add(switchPort);

            });

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    public List<Integer> getPortIndexBySwitch(String switchIp) {

        List<Integer> list = new ArrayList<>();

        try {

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.2.2.1.1"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {

                        int index = varBinding.getVariable().toInt();
                        list.add(index);

                    } catch (Exception ex) {
                    }

                }

            }

            snmp.close();
            transport.close();

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    public Map<Integer, String> getIndexDescriptionBySwitch(String switchIp) {

        Map<Integer, String> list = new HashMap<>();

        try {

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.2.2.1.2"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {

                        String description = varBinding.getVariable().toString();

                        String partsOid[] = varBinding.getOid().toString().split("\\.");
                        int index = Integer.parseInt(partsOid[partsOid.length - 1]);

                        list.put(index, description);

                    } catch (Exception ex) {
                    }

                }

            }

            snmp.close();
            transport.close();

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    public Map<Integer, Integer> getPortIndexRefBySwitch(String switchIp) {

        Map<Integer, Integer> list = new HashMap<>();

        try {

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.17.1.4.1.2"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {

                        int index = varBinding.getVariable().toInt();

                        String partsOid[] = varBinding.getOid().toString().split("\\.");
                        int indexRef = Integer.parseInt(partsOid[partsOid.length - 1]);

                        list.put(index, indexRef);

                    } catch (Exception ex) {
                    }

                }

            }

            snmp.close();
            transport.close();

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    public List<DeviceConected> getTableArp(String switchIp) {

        List<DeviceConected> list = new ArrayList<>();

        try {

            SnmpCommunity community = checkCommunities(switchIp, communitiesByConfig);

            if (community == null) {
                return list;
            }

            CommunityTarget target = getTarget(switchIp, community);

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(".1.3.6.1.2.1.4.22.1.2"));

            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    continue;
                }
                VariableBinding[] varBindings = event.getVariableBindings();
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }
                    try {
                        String ip = getPartByOid(varBinding.getOid().toString());
                        String mac = varBinding.getVariable().toString();
                        list.add(new DeviceConected(ip, mac));
                    } catch (Exception ex) {
                    }
                }
            }

            snmp.close();
            transport.close();

            pushVendorAndTypeDevice(list);

            return list;

        } catch (Exception ex) {
            return list;

        }

    }

    private List<DeviceConected> addDevicesConected(
            String switchIp,
            List<DeviceConected> listDevicesConected,
            List<String> history) {

        if (history.contains(switchIp)) {
            return listDevicesConected;
        }

        history.add(switchIp);

        List<DeviceConected> listDevicesConectedBySwitche = getTableArp(switchIp);

        listDevicesConected.addAll(listDevicesConectedBySwitche);

        for (DeviceConected device : listDevicesConectedBySwitche) {
            if (!device.isSwitch()) {
                continue;
            }
            addDevicesConected(device.getIp(), listDevicesConected, history);
        }

        return listDevicesConected;

    }

    @Override
    public void run() {

        while (run) {

            // todos os devices conectados na rede
            System.out.println("Coletando devices via tabela ARP ...");
            List<DeviceConected> listDevicesConected = new ArrayList<>();
            for (Switch swt : switchsByConfig) {
                listDevicesConected = addDevicesConected(swt.getIp(), listDevicesConected, new ArrayList<>());
            }
            listDevicesConected = listDevicesConected.stream()
                    .filter(Util.distinctByKey(k -> k.getMac()))
                    .collect(Collectors.toList());
            System.out.println("*** DEVICES ***");
            listDevicesConected.forEach(dev -> {
                System.out.println("DEVICE: IP: " + dev.getIp() + ", MAC: " + dev.getMac());
            });
            System.out.println("-----------------------------------------------------------");

            // todos os switches
            System.out.println("Filtrando os switches ...");
            List<Switch> listSwitchs = listDevicesConected.stream()
                    .filter(d -> d.isSwitch())
                    .map(d -> new Switch(d.getIp(), d.getMac(), d.getDescription()))
                    .collect(Collectors.toList());
            System.out.println("*** SWITCHES ***");
            listSwitchs.forEach(swt -> {
                System.out.println("SWITCH: IP: " + swt.getIp() + ", MAC: " + swt.getMac());
            });
            System.out.println("-----------------------------------------------------------");

            // coletando os devices dos switches
            System.out.println("Coletando os devices dos switches ...");
            for (Switch swt : listSwitchs) {
                List<SwitchPort> listPortsAndDevicesConected = getPortsAndDevicesConected(swt.getIp(), listDevicesConected);
                swt.setPorts(listPortsAndDevicesConected);
            }
            System.out.println("-----------------------------------------------------------");

            // printando ..            
            System.out.println("*** LISTANDO SWITCHES, PORTAS E DEVICES ***");
            for (Switch swt : listSwitchs) {
                List<SwitchPort> listPortsAndDevicesConected = swt.getPorts();
                System.out.println("# SWITCH: " + swt.getIp());
                listPortsAndDevicesConected.forEach(swtPort -> {
                    System.out.println("PORT: " + swtPort.getDescription() + " / TOTAL_DEVICES: " + swtPort.getDevices().size());
                });
                System.out.println("\n");
            }
            System.out.println("-----------------------------------------------------------");
            System.out.println("END");

            for (Switch swt : listSwitchs) {
                swt.getPorts().forEach(swtPort -> {
                    swtPort.getDevices().forEach(dev -> {
                        sendData(swt, swtPort, dev);
                    });
                });
            }

            try {
                Thread.sleep(config.getBeat().getInterval() * 1000);
            } catch (InterruptedException ex) {
            }

        }

    }

    private JSONObject getJsonDefault() {

        JSONObject attributesDefault = new JSONObject();

        try {

            attributesDefault.put("timestamp", Util.getDatetimeNowForElasticsearch());

            JSONObject metadataJSON = new JSONObject();
            metadataJSON.put("beat", config.getBeat().getName());
            metadataJSON.put("version", config.getBeat().getVersion());
            attributesDefault.put("@metadata", metadataJSON);

            JSONObject beatJSON = new JSONObject();
            beatJSON.put("name", config.getBeat().getName());
            beatJSON.put("version", config.getBeat().getVersion());
            attributesDefault.put("beat", beatJSON);

            attributesDefault.put("tags", config.getBeat().getTags());

            attributesDefault.put("client_name", config.getBeat().getClientName());
            attributesDefault.put("client_id", config.getBeat().getClientId());

        } catch (JSONException ex) {
        }

        return attributesDefault;

    }

    private void sendData(Switch swt, SwitchPort port, DeviceConected dev) {

        try {

            JSONObject swtJson = new JSONObject();
            swtJson.put("ip", swt.getIp());
            swtJson.put("mac", swt.getMac());
            swtJson.put("description", swt.getDescription());

            JSONObject portJson = new JSONObject();
            portJson.put("index", port.getIndex());
            portJson.put("description", port.getDescription());

            JSONObject deviceJson = new JSONObject();
            deviceJson.put("ip", dev.getIp());
            deviceJson.put("mac", dev.getMac());
            deviceJson.put("description", dev.getDescription());
            deviceJson.put("category", dev.getCategory());
            deviceJson.put("type", dev.getType());
            deviceJson.put("vendor", dev.getVendor());

            JSONObject objJson = new JSONObject();
            objJson.put("switch", swtJson);
            objJson.put("port", portJson);
            objJson.put("device", deviceJson);
            
            objJson.put("type", "DeviceInPort");

            JSONObject json = Util.getJson(getJsonDefault(), objJson);
            Logger.getLogger(ReadSend.class.getName())
                    .log(Level.INFO, Util.prettyPrinter(json));

            try (Socket socket = new Socket(output.getHost(), output.getPort());
                    DataOutputStream os = new DataOutputStream(
                            new BufferedOutputStream(socket.getOutputStream()))) {
                os.writeBytes(json.toString());
                os.flush();
            } catch (Exception ex) {
                Logger.getLogger(ReadSend.class.getName())
                        .log(Level.WARNING, "Failed to send data ", ex);
            }
            
            //==
            
            objJson.put("type", "LatestDeviceInPort");
            objJson.put("id_doc", "switch_" + swt.getMac() + "_device_" + dev.getMac());

            json = Util.getJson(getJsonDefault(), objJson);
            Logger.getLogger(ReadSend.class.getName())
                    .log(Level.INFO, Util.prettyPrinter(json));

            try (Socket socket = new Socket(output.getHost(), output.getPort());
                    DataOutputStream os = new DataOutputStream(
                            new BufferedOutputStream(socket.getOutputStream()))) {
                os.writeBytes(json.toString());
                os.flush();
            } catch (Exception ex) {
                Logger.getLogger(ReadSend.class.getName())
                        .log(Level.WARNING, "Failed to send data ", ex);
            }

        } catch (Exception ex) {
        }

    }

}
