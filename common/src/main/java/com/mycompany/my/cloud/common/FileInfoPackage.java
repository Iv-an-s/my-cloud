package com.mycompany.my.cloud.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileInfoPackage implements Serializable {
    private static final long serialVersionUID = 12345L;
    private List<FileInfo> fileInfoList;

    public FileInfoPackage(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }
}
