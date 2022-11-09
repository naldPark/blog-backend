package me.nald.blog.service;


import lombok.RequiredArgsConstructor;
import me.nald.blog.util.FileUtils;
import me.nald.blog.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorageService {


    public Path postWorkspaceDownloads() throws IOException {
//        Path workspaceBackupPath = Paths.get("/naldStorage").resolve(Util.getUUID());

        Path workspaceBackupPath = Paths.get("/naldStorage").resolve("locate");


//        Path zipFilePath = Paths.get("/naldstorage").resolve(Util.getUUIDWithoutDash() + "nald." + "zip");
        Path zipFilePath = Paths.get("/naldstorage").resolve("sample." + "mp4");

//            FileUtils.compressToZip(workspaceBackupPath.toString(), zipFilePath.toString());
//            FileUtils.deletePath(workspaceBackupPath.toString());
        return zipFilePath;
    }

}

