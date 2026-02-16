package com.example.excelimport.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String root = "./storage";
    private int retentionDays = 30;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
}
