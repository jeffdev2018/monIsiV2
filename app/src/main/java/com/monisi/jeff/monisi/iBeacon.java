package com.monisi.jeff.monisi;


public class iBeacon
{
    private int major;
    private int minor;
    private int rssi;
    private int txPower;

    private String UUID;

    public iBeacon() {this("", -1, -1, 0, 0);}

    public iBeacon(String uuid, int major, int minor, int rssi, int txPower)
    {
        this.major = major;
        this.minor = minor;
        this.rssi = rssi;
        this.txPower = txPower;
        this.UUID = uuid;
    }

    public iBeacon build() {return new iBeacon(UUID, major, minor, rssi, txPower);}

    public void setMajor(int m) {major = m;}
    public void setMinor(int m) {minor = m;}
    public void setRssi(int r) {rssi = r;}
    public void setTxPower(int tx) {txPower = tx;}
    public void setUUID(String U) {UUID = U;}

    public int getMajor() {return major;}
    public int getMinor() {return minor;}
    public int getRssi() {return rssi;}
    public int getTxPower() {return txPower;}
    public String getUUID() {return UUID;}

    @Override
    public String toString()
    {
        return ("UUID: " + UUID + "\n"
                + "Major: " + major + "\n"
                + "Minor: " + minor + "\n"
                + "RSSI+ " + rssi + "\n");
    }

    public String toLoggedData()
    {
        return ("" + major + "\t" + rssi);
    }
}