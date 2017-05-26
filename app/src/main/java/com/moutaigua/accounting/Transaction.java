package com.moutaigua.accounting;

/**
 * Created by mou on 5/6/17.
 */

public class Transaction {

    public static String TYPE_ME = "Me";
    public static String TYPE_ME_AND_OTHER = "Me & Other";
    public static String TYPE_OTHER = "Other";


    private long longTime;
    private String textTime;
    private TransactionCategory category;
    private String money;
    private String providerName;
    private String city;
    private double gpsLongitude;
    private double gpsLatitude;
    private String note;
    private String type;
    private int seperate;
    private String reportSource;
    private String reportId;

    public Transaction(){
        longTime = 0;
        textTime = "";
        category = new TransactionCategory();
        money = "0.00";
        providerName = "";
        city = "";
        gpsLatitude = 0;
        gpsLongitude = 0;
        type = TYPE_ME;
        seperate = 1;
        reportSource = "";
        reportId = "";
    }


    public void clear(){
        longTime = 0;
        textTime = "";
        category.clear();
        money = "0.00";
        providerName = "";
        city = "";
        gpsLatitude = 0;
        gpsLongitude = 0;
        type = TYPE_ME;
        seperate = 1;
        reportSource = "";
        reportId = "";
    }

    public long getLongTime() {
        return longTime;
    }

    public void setLongTime(long longTime) {
        this.longTime = longTime;
    }

    public String getTextTime() {
        return textTime;
    }

    public void setTextTime(String textTime) {
        this.textTime = textTime;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(double gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public double getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(double gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSeperate() {
        return seperate;
    }

    public void setSeperate(int seperate) {
        this.seperate = seperate;
    }

    public String getReportSource() {
        return reportSource;
    }

    public void setReportSource(String reportSource) {
        this.reportSource = reportSource;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public static class TransactionCategory {
        private String code;
        private String name;
        private int index;

        public TransactionCategory(){
            name = "";
            code = "";
            index = 0;
        }

        public void clear(){
            name = "";
            code = "";
            index = 0;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

    }

}



