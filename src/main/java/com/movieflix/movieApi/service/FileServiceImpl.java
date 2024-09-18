package com.movieflix.movieApi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String uploadFile(String path, MultipartFile file) throws IOException {

        String filename=file.getOriginalFilename();

        String filepath=path + File.separator +filename;

        //Create a File object
        File f=new File(path);

        if(!f.exists()) {
            f.mkdir();
        }

        //copy the file or upload file to the path
        Files.copy(file.getInputStream(), Paths.get(filepath));
        return filename;
    }

    @Override
    public InputStream getResourceFile(String path, String filename) throws FileNotFoundException {
        String filepath=path+ File.separator+ filename;
        return new FileInputStream(filepath);
    }
}
